class UrlMappings {
    static mappings = {
        "/test" { controller = 'test'; action = [ POST: "dostuff" ] }
        "/test/$testId" { controller = 'test'; action = [ POST: "dootherstuff" ] }
    }
}
