package net.ririfa.langman

enum class InitType(val experimental: Boolean) {
	YAML(false),
	JSON(true);

	//TODO: TOMLを実装したい
}
