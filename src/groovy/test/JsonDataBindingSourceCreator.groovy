package test

import org.codehaus.groovy.grails.web.binding.bindingsource.*

import groovy.transform.CompileStatic

import java.util.regex.Pattern

import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.databinding.CollectionDataBindingSource
import org.grails.databinding.DataBindingSource
import org.grails.databinding.SimpleMapDataBindingSource
import org.grails.databinding.bindingsource.AbstractRequestBodyDataBindingSourceCreator
import org.grails.databinding.bindingsource.DataBindingSourceCreationException
import org.grails.databinding.bindingsource.InvalidRequestBodyException
import org.springframework.beans.factory.annotation.Autowired

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonReader

/**
 * Creates DataBindingSource objects from JSON in the request body
 *
 * @since 2.3
 * @author Jeff Brown
 * @author Graeme Rocher
 *
 * @see DataBindingSource
 * @see org.grails.databinding.bindingsource.DataBindingSourceCreator
 */
@CompileStatic
class JsonDataBindingSourceCreator extends AbstractRequestBodyDataBindingSourceCreator {

    private static final Pattern INDEX_PATTERN = ~/^(\S+)\[(\d+)\]$/

    @Autowired(required = false)
    Gson gson = new Gson()

    @Override
    MimeType[] getMimeTypes() {
        [MimeType.JSON, MimeType.TEXT_JSON] as MimeType[]
    }

    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) {
        println ">> $mimeType, $bindingTargetType, $bindingSource"
        DataBindingSource result = null

        if(bindingSource instanceof JsonObject) {
            result = new SimpleMapDataBindingSource(createJsonObjectMap((JsonObject)bindingSource))
        }
        else if(bindingSource instanceof JSONObject) {
            result = new SimpleMapDataBindingSource((JSONObject)bindingSource)
        }
        else {
            result = super.createDataBindingSource(mimeType, bindingTargetType, bindingSource)
        }
        println ">> result: ${result.propertyNames}"
        return result
    }

    @Override
    protected CollectionDataBindingSource createCollectionBindingSource(Reader reader) {
        println ">> createCollectionBindingSource"
        def jsonReader = new JsonReader(reader)
        jsonReader.setLenient true
        def parser = new JsonParser()

        // TODO Need to decide what to do if the root element is not a JsonArray
        JsonArray jsonElement = (JsonArray)parser.parse(jsonReader)
        def dataBindingSources = jsonElement.collect { JsonElement element ->
            new SimpleMapDataBindingSource(createJsonObjectMap(element))
        }
        return new CollectionDataBindingSource() {
            List<DataBindingSource> getDataBindingSources() {
                dataBindingSources
            }
        }
    }

    @Override
    protected DataBindingSource createBindingSource(Reader reader) {
        println ">> createBindingSource ${reader.class}"
        def jsonReader = new JsonReader(reader)
        jsonReader.setLenient true
        def parser = new JsonParser()
        final jsonElement = parser.parse(jsonReader)

        Map result = createJsonObjectMap(jsonElement)

        println ">> result"

        return new SimpleMapDataBindingSource(result)

    }

    /**
     * Returns a map for the given JsonElement. Subclasses can override to customize the format of the map
     *
     * @param jsonElement The JSON element
     * @return The map
     */
    protected Map createJsonObjectMap(JsonElement jsonElement) {
        Map newMap = [:]
        if(jsonElement instanceof JsonObject) {
            def jom = new JsonObjectMap(jsonElement, gson)
            jom.each { k, v ->
                def newValue = v instanceof JsonElement ? getValueForJsonElement((JsonElement)v, gson) : v
                newMap[k] = newValue
            }   
        }
        newMap
    }

    Object getValueForJsonElement(JsonElement value, Gson gson) {
        if (value == null || value.isJsonNull()) {
            return null
        }

        if (value.isJsonPrimitive()) {
            JsonPrimitive prim = (JsonPrimitive) value
            if (prim.isNumber()) {
                return value.asNumber
            }
            if (prim.isBoolean()) {
                return value.asBoolean
            }
            return value.asString
        }

        if (value.isJsonObject()) {
            return createJsonObjectMap((JsonObject) value)
        }

        if (value.isJsonArray()) {
            return new JsonArrayList((JsonArray)value, gson)
        }
    }

    @CompileStatic
    class JsonObjectMap implements Map {

        JsonObject jsonObject
        Gson gson

        JsonObjectMap(JsonObject jsonObject, Gson gson) {
            this.jsonObject = jsonObject
            this.gson = gson
        }

        int size() {
            jsonObject.entrySet().size()
        }

        boolean isEmpty() {
            jsonObject.entrySet().isEmpty()
        }

        boolean containsKey(Object o) {
            jsonObject.has(o.toString())
        }

        boolean containsValue(Object o) {
            get(o) != null
        }

        Object get(Object o) {
            final key = o.toString()
            final value = jsonObject.get(key)
            if(value != null) {
                return getValueForJsonElement(value, gson)
            }
            else {
                final matcher = INDEX_PATTERN.matcher(key)
                if(matcher.find()) {
                    String newKey = matcher.group(1)
                    final listValue = jsonObject.get(newKey)
                    if(listValue.isJsonArray()) {
                        JsonArray array = (JsonArray)listValue
                        int index = matcher.group(2).toInteger()
                        getValueForJsonElement(array.get(index), gson)
                    }
                }
            }
        }


        Object put(Object k, Object v) {
            jsonObject.add(k.toString(), gson.toJsonTree(v))
        }

        Object remove(Object o) {
            jsonObject.remove(o.toString())
        }

        void putAll(Map map) {
            for(entry in map.entrySet()) {
                put(entry.key, entry.value)
            }
        }

        void clear() {
            for(entry in entrySet())  {
                remove(entry.key)
            }
        }

        Set keySet() {
            jsonObject.entrySet().collect{ Map.Entry entry -> entry.key }.toSet()
        }

        Collection values() {
            jsonObject.entrySet().collect{ Map.Entry entry -> entry.value}
        }

        Set<Map.Entry> entrySet() {
            jsonObject.entrySet()
        }
    }

    @CompileStatic
    class JsonArrayList extends AbstractList {

        JsonArray jsonArray
        Gson gson

        JsonArrayList(JsonArray jsonArray, Gson gson) {
            this.jsonArray = jsonArray
            this.gson = gson
        }

        int size() {
            jsonArray.size()
        }

        Object get(int i) {
            final jsonElement = jsonArray.get(i)
            return getValueForJsonElement(jsonElement, gson)
        }
    }
    
    @Override
    protected DataBindingSourceCreationException createBindingSourceCreationException(Exception e) {
        if(e instanceof JsonParseException) {
            return new InvalidRequestBodyException(e)
        }
        return super.createBindingSourceCreationException(e)
    }
}