package test

@grails.validation.Validateable
class FieldACommand {
    String fieldA

    static constraints = {
        fieldA nullable: false
    }
}

