package org.cosmic.ide.project;

public class CodeTemplate {
		
	public static String getJavaClassTemplate(
	String packageName, String className, boolean isCreateMainMethod,  String classType) {		
		
		var simpleJavaClass = 
		"public class "
		+ className
		+ " {\n"
		+ (isCreateMainMethod
		? "\tpublic static void main(String[] args) {\n"
		+ "\t\tSystem.out.println(\"Hello, World!\");\n"
		+ "\t}"
		: "    ")
		+ "\n"
		+ "}\n";
		
		var simpleJavaInterface =
		"interface "
		+ className
		+ " {\n"		
		+ "\n"
		+ "}\n";
		
		var simpleJavaAbstract =
		"public abstract class "
		+ className
		+ " {\n"		
		+ "\n"
		+ "}\n";
		
		var simpleJavaEnum =
		"enum "
		+ className
		+ " {\n"		
		+ "\t"
		+ "}\n";
		
		var header = "";
		var classBody = header + simpleJavaClass;
		
		if (!isEmpty(packageName)) {
			header = "package " + packageName + ";\n" + "\n";
		}
		
		switch (classType) {
			case "Class":
				classBody = header + simpleJavaClass;
			break;
			case "Interface":
				classBody = header + simpleJavaInterface;
			break;
			case "Abstract":
				classBody = header + simpleJavaAbstract;
			break;	
			case "Enum":
				classBody = header + simpleJavaEnum;
			break;
		}
		
		return classBody;
	}
	
	public static String getKotlinClassTemplate(
	String packageName, String className, boolean isCreateMainMethod, String classType) {
		
		var simpleKotlinClass =
		"class "
		+ className
		+ " {\n\t"
		+ (isCreateMainMethod
		? "fun main(args: Array<String>) {\n"
		+ "\t\tprintln(\"Hello, World!\")\n"
		+ "\t}"
		: "\t")
		+ "\n"
		+ "}\n";
		
		var simpleKotlinDataClass =
		"data class " + className + "()"
		+ "\n";
		
		var simpleKotlinObjectDeclaration =
		"object " + className
		+ " {\n"
		+ "\n"
		+ "}\n";

		var simpleKotlinInterface =
		"interface "
		+ className
		+ " {\n"		
		+ "\n"
		+ "}\n";

		var simpleKotlinAbstract =
		"abstract class "
		+ className
		+ " {\n"		
		+ "\n"
		+ "}\n";

		var simpleKotlinEnum =
		"enum class "
		+ className
		+ " {\n"
		+ "\n"
		+ "}\n";

		var header = "";
		var classBody = header + simpleKotlinClass;

		if (!isEmpty(packageName)) {
			header = "package " + packageName + "\n" + "\n";
		}

		switch (classType) {
			case "Data":
				classBody = header + simpleKotlinDataClass;
			break;
			case "Object":
				classBody = header + simpleKotlinObjectDeclaration;
			break;			
			case "Interface":
				classBody = header + simpleKotlinInterface;
			break;
			case "Abstract":
				classBody = header + simpleKotlinAbstract;
			break;	
			case "Enum":
				classBody = header + simpleKotlinEnum;
			break;
		}
		return classBody;
	}
	
	public static boolean isEmpty(final CharSequence s) {
		return s == null || s.length() <= 0;
	}
}
