COMPILE_DEPS = CORE_DEPS + KRYO + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//core/store/primitives:onos-core-primitives",
    "//utils/rest:onlab-rest-native",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "Graph Brent Thesis",
    title = "Graph Brent Thesis",
    url = "http://onosproject.org",
)
