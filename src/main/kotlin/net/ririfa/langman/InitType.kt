package net.ririfa.langman

enum class InitType(val experimental: Boolean) {
	YAML(false),
	JSON(true),
	PROPERTIES(true),
	TOML(true);
}
