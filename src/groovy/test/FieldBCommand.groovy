package test

@grails.validation.Validateable
class FieldBCommand {
    String fieldB

    static constraints = {
        fieldB nullable: false
    }
}

