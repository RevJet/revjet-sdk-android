# Nothing in the SDK is reached by reflection, from a layout file or by name from JavaScript, so
# R8 can trace the whole library from what the application itself calls. A blanket keep on
# com.revjet.sdk.** would keep the internals too: Kotlin's `internal` is public in the class file.
