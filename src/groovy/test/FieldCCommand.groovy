package test

@grails.validation.Validateable
class FieldCCommand {
    Long testId
    String data1
    String data2

    static constraints = {
        testId nullable: false
        data1 nullable: true
        data2 nullable: true
    }
}

