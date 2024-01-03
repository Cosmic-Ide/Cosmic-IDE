package io.appwrite

class Query {
    companion object {
        fun equal(attribute: String, value: Any) = addQuery(attribute, "equal", value)

        fun notEqual(attribute: String, value: Any) = Query.addQuery(attribute, "notEqual", value)

        fun lessThan(attribute: String, value: Any) = Query.addQuery(attribute, "lessThan", value)

        fun lessThanEqual(attribute: String, value: Any) =
            Query.addQuery(attribute, "lessThanEqual", value)

        fun greaterThan(attribute: String, value: Any) =
            Query.addQuery(attribute, "greaterThan", value)

        fun greaterThanEqual(attribute: String, value: Any) =
            Query.addQuery(attribute, "greaterThanEqual", value)

        fun search(attribute: String, value: String) = Query.addQuery(attribute, "search", value)

        fun isNull(attribute: String) = "isNull(\"${attribute}\")"

        fun isNotNull(attribute: String) = "isNotNull(\"${attribute}\")"

        fun between(attribute: String, start: Int, end: Int) =
            "between(\"${attribute}\", ${start}, ${end})"

        fun between(attribute: String, start: Double, end: Double) =
            "between(\"${attribute}\", ${start}, ${end})"

        fun between(attribute: String, start: String, end: String) =
            "between(\"${attribute}\", \"${start}\", \"${end}\")"

        fun startsWith(attribute: String, value: String) =
            Query.addQuery(attribute, "startsWith", value)

        fun endsWith(attribute: String, value: String) =
            Query.addQuery(attribute, "endsWith", value)

        fun select(attributes: List<String>) =
            "select([${attributes.joinToString(",") { "\"$it\"" }}])"

        fun orderAsc(attribute: String) = "orderAsc(\"${attribute}\")"

        fun orderDesc(attribute: String) = "orderDesc(\"${attribute}\")"

        fun cursorBefore(documentId: String) = "cursorBefore(\"${documentId}\")"

        fun cursorAfter(documentId: String) = "cursorAfter(\"${documentId}\")"

        fun limit(limit: Int) = "limit(${limit})"

        fun offset(offset: Int) = "offset(${offset})"

        private fun addQuery(attribute: String, method: String, value: Any): String {
            return when (value) {
                is List<*> -> "${method}(\"${attribute}\", [${
                    value.map { it -> parseValues(it!!) }.joinToString(",")
                }])"

                else -> "${method}(\"${attribute}\", [${Query.parseValues(value)}])"
            }
        }

        private fun parseValues(value: Any): String {
            return when (value) {
                is String -> "\"${value}\""
                else -> "${value}"
            }
        }
    }
}
