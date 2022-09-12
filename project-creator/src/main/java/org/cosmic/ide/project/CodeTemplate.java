package org.cosmic.ide.project;

public class CodeTemplate {
		
	public static String getJavaClassTemplate(
	String packageName, String className, boolean isCreateMainMethod,  String classType) {		
		
		String simpleJavaClass = 
		"import java.util.*;\n\n"
		+ "public class "
		+ className
		+ " {\n"
		+ (isCreateMainMethod
		? "\tpublic static void main(String[] args) {\n"
		+ "\t\tSystem.out.println(\"Hello, World!\");\n"
		+ "\t}"
		: "    ")
		+ "\n"
		+ "}\n";
		
		String simpleJavaInterface =
		"import java.util.*;\n\n"
		+ "interface "
		+ className
		+ " {\n"		
		+ "\n"
		+ "}\n";
		
		String simpleJavaAbstract =
		"import java.util.*;\n\n"
		+ "public abstract "
		+ className
		+ " {\n"		
		+ "\n"
		+ "}\n";
		
		String simpleJavaEnum =
		"import java.util.*;\n\n"
		+ "enum "
		+ className
		+ " {\n"		
		+ "\n"
		+ "}\n";
		
		String header = "";
		String classBody = header + simpleJavaClass;
		
		if (!isEmpty(packageName)) {
			header = "package " + packageName + ";\n" + "\n";
		}
		
		switch(classType){
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
		
		String simpleKotlinClass =
		"import java.util.*\n\n"
		+ "class "
		+ className
		+ " {\n\t"
		+ (isCreateMainMethod
		? "fun main(args: Array<String>) {\n"
		+ "\t\tprintln(\"Hello, World!\")\n"
		+ "\t}"
		: "    ")
		+ "\n"
		+ "}\n";
		
		String simpleKotlinDataClass =
		"import java.util.*;\n\n"
		+ "data class " + className + "(val name: String, val age: Int) "		
		+ "\n";
		
		String simpleKotlinObjectDeclaration =
		"import java.util.*;\n\n"
		+ "object " + className 
		+ " {\n"
		+ "\n"
		+ "}\n";
		
		String simpleKotlinInterface =
		"import java.util.*;\n\n"
		+ "interface "
		+ className
		+ " {\n"		
		+ "\n"
		+ "}\n";
		
		String simpleKotlinAbstract =
		"import java.util.*;\n\n"
		+ "abstract class "
		+ className
		+ " {\n"		
		+ "\n"
		+ "}\n";
		
		String simpleKotlinEnum =
		"import java.util.*;\n\n"
		+ "enum class "
		+ className
		+ " {\n"		
		+ "\n"
		+ "}\n";		
		
		String header = "";
		String classBody = header + simpleKotlinClass;
		
		if (!isEmpty(packageName)) {
			header = "package " + packageName + "\n" + "\n";
		}
		
		switch(classType){
			case "Class":
				classBody = header + simpleKotlinClass;
			break;
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
		return s == null || s.length() == 0;
	}
}