package test

import grails.converters.JSON

class TestController {
    def dostuff(FieldACommand cmdA, FieldBCommand cmdB){
        def result = [
            "testA" : cmdA.fieldA,
            "testB" : cmdB.fieldB
        ]
        render result as JSON
    }

    def dootherstuff(FieldCCommand cmd) {
        def result = cmd.properties
        render result as JSON
    }
}
