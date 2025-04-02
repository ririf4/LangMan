package net.ririfa.langman

/**
 * Represents different initialization file types supported by the system.
 *
 * @property fileExtension The file extension associated with the initialization type.
 */
enum class InitType(val fileExtension: String) {
	/**
	 * Represents the YAML initialization type used to define configurations
	 * with files that have a `.yml` extension.
	 */
	YAML("yml"),

	/**
	 * Represents a JSON initialization file type used by the system.
	 *
	 * This file type is associated with the ".json" file extension.
	 */
	JSON("json"),

	/**
	 * Represents the TOML file type as an option for initialization file configurations.
	 * Used to handle initialization files with the `.toml` file extension.
	 */
	TOML("toml");
}
