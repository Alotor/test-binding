class UrlMappings {
    static mappings = {
        "/test" { controller = 'test'; action = [ POST: "dostuff" ] }
    }
}
