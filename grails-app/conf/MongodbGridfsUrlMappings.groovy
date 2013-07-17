class MongodbGridfsUrlMappings {
	static mappings = {
		"/thumb/$id/$width/$height"(controller:"gridfs", action:"showIcon")
	}
}
