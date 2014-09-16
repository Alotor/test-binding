package test

import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.databinding.DataBindingSource
import javax.servlet.http.HttpServletRequest
import org.grails.databinding.SimpleMapDataBindingSource
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

@CompileStatic
class JsonDataBindingSourceCreator extends org.codehaus.groovy.grails.web.binding.bindingsource.JsonDataBindingSourceCreator {
    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) {
        DataBindingSource result = super.createDataBindingSource(mimeType, bindingTargetType, bindingSource)

        if (bindingSource instanceof HttpServletRequest && result instanceof SimpleMapDataBindingSource){
            HttpServletRequest httpServletRequest = (HttpServletRequest)bindingSource
            Map paramsMap = ((SimpleMapDataBindingSource)result).map
            final GrailsWebRequest grailsWebRequest = GrailsWebRequest.lookup(httpServletRequest)
            final GrailsParameterMap parameterMap = grailsWebRequest.getParams()
            paramsMap.putAll(parameterMap)
        }
        return result
    }
}