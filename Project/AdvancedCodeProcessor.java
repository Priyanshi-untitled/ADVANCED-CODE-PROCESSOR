import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
public class AdvancedCodeProcessor {
    private static final String OUTPUT_DIR = "processed_code";
    private static final String REPORT_FILE = "processing_report.txt";
    private static final String[] SUPPORTED_EXTENSIONS = {".java", ".py", ".cpp", ".js"};
    private static class CodeStructure {
        String originalCode;
        String fileExtension;
        List<String> imports;
        List<String> classes;
        List<String> methods;
        List<String> variables;
        int lineCount;
        Map<String, Integer> methodLineCounts;
        Map<String, String> methodSignatures;
        Map<String, Integer> variableUsage;
        Map<String, String> methodParameters;
        Map<String, String> methodReturnTypes;
        CodeStructure(String code, String ext) {
            this.originalCode = code;
            this.fileExtension = ext;
            this.imports = new ArrayList<>();
            this.classes = new ArrayList<>();
            this.methods = new ArrayList<>();
            this.variables = new ArrayList<>();
            this.lineCount = code.split("\n").length;
            this.methodLineCounts = new HashMap<>();
            this.methodSignatures = new HashMap<>();
            this.variableUsage = new HashMap<>();
            this.methodParameters = new HashMap<>();
            this.methodReturnTypes = new HashMap<>();}
    }
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter directory path to process (or 'exit' to quit):");
        while (true) {
            String inputPath = scanner.nextLine().trim();
            if (inputPath.equalsIgnoreCase("exit")) {
                System.out.println("Exiting program.");
                break;}
            System.out.println("Enter search term (regex, or press Enter to skip):");
            String searchTerm = scanner.nextLine().trim();
            System.out.println("Enter filter type (imports/classes/methods/variables/method_name/variable_type/parameter_type/return_type/all, or press Enter for all):");
            String filterType = scanner.nextLine().trim().toLowerCase();
            System.out.println("Filter value (e.g., method name, variable type, parameter type, return type, or press Enter to skip):");
            String filterValue = scanner.nextLine().trim();
            System.out.println("Concatenate all files? (yes/no):");
            boolean concatenate = scanner.nextLine().trim().equalsIgnoreCase("yes");
            System.out.println("Enter text to replace (or press Enter to skip):");
            String replaceFrom = scanner.nextLine().trim();
            String replaceTo = "";
            if (!replaceFrom.isEmpty()) {
                System.out.println("Enter replacement text:");
                replaceTo = scanner.nextLine().trim();}
            System.out.println("Format indentation? (yes/no):");
            boolean formatIndent = scanner.nextLine().trim().equalsIgnoreCase("yes");
            System.out.println("Transform identifiers to (uppercase/lowercase/none):");
            String transformCase = scanner.nextLine().trim().toLowerCase();
            try {
                validateInputPath(inputPath);
                processDirectory(inputPath, searchTerm, filterType, filterValue, concatenate, replaceFrom, replaceTo, formatIndent, transformCase);
                System.out.println("Processing complete. Enter another directory path or 'exit':");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                logError("Main loop error: " + e.getMessage());
                System.out.println("Enter a valid directory path or 'exit':");}
        }
        scanner.close();}
    private static void processDirectory(String dirPath, String searchTerm, String filterType, String filterValue, boolean concatenate, String replaceFrom, String replaceTo, boolean formatIndent, String transformCase) throws IOException {
        File dir = new File(dirPath);
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        File[] files = dir.listFiles((d, name) -> isSupportedFile(name));
        if (files == null || files.length == 0) {
            System.out.println("No supported files found.");
            logError("No supported files in directory: " + dirPath);
            return;}
        List<CodeStructure> structures = new ArrayList<>();
        StringBuilder report = new StringBuilder();
        report.append("Processing Report - ").append(new Date()).append("\n");
        for (File file : files) {
            logProcessingStart(file);
            CodeStructure structure = processFile(file, searchTerm, filterType, filterValue);
            if (structure != null) {
                structures.add(structure);
                report.append(generateFileReport(file, structure));}
            logProcessingEnd(file);}
        saveReport(report.toString());
        if (concatenate) {
            concatenateFiles(structures, replaceFrom, replaceTo, formatIndent, transformCase);
        } else {
            saveIndividualFiles(structures, replaceFrom, replaceTo, formatIndent, transformCase);
        }
    }
    private static boolean isSupportedFile(String fileName) {
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (fileName.toLowerCase().endsWith(ext)) {
                return true;}
        }
        return false;}
    private static CodeStructure processFile(File file, String searchTerm, String filterType, String filterValue) throws IOException {
        String code = new String(Files.readAllBytes(file.toPath()));
        if (!searchTerm.isEmpty() && !matchesSearchTerm(code, searchTerm)) {
            return null;}
        String extension = getFileExtension(file.getName());
        CodeStructure structure = extractStructure(code, extension);
        System.out.println("File: " + file.getName() + ", Lines: " + structure.lineCount);
        structure.originalCode = removeBlankLines(structure.originalCode);
        structure.originalCode = removeDuplicateLines(structure.originalCode);
        structure.methods = removeDuplicateMethods(structure);
        analyzeVariableUsage(structure);
        filterStructure(structure, filterType, filterValue);
        return structure;}
    private static boolean matchesSearchTerm(String code, String searchTerm) {
        if (!isValidRegex(searchTerm)) {
            return code.contains(searchTerm);
        }
        try {
            return Pattern.compile(searchTerm).matcher(code).find();
        } catch (PatternSyntaxException e) {
            logError("Invalid regex pattern: " + searchTerm);
            return false;}
    }
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex == -1 ? "" : fileName.substring(dotIndex).toLowerCase();}
    private static String removeBlankLines(String code) {
        return code.replaceAll("(?m)^\\s*$[\n\r]+", ""); }
    private static String removeDuplicateLines(String code) {
        String[] lines = code.split("\n");
        Set<String> uniqueLines = new LinkedHashSet<>();
        for (String line : lines) {
            uniqueLines.add(line.trim());
        }
        return String.join("\n", uniqueLines); }
    private static List<String> removeDuplicateMethods(CodeStructure structure) {
        Map<String, String> uniqueMethods = new HashMap<>();
        List<String> result = new ArrayList<>();
        for (String method : structure.methods) {
            String signature = extractMethodSignature(method, structure.fileExtension);
            if (!uniqueMethods.containsKey(signature)) {
                uniqueMethods.put(signature, method);
                result.add(method);
                structure.methodSignatures.put(method, signature);}
        }
        return result;}
    private static String extractMethodSignature(String method, String extension) {
        switch (extension) {
            case ".java":
                return method.replaceAll("(?s)\\{.*\\}", "").trim();
            case ".py":
                return method.replaceAll("(?s):[\\s\\S]*", "").trim();
            case ".cpp":
                return method.replaceAll("(?s)\\{.*\\}", "").trim();
            case ".js":
                return method.replaceAll("(?s)\\{.*\\}", "").trim();
            default:
                return method;}
    }
    private static void analyzeVariableUsage(CodeStructure structure) {
        for (String variable : structure.variables) {
            String varName = extractVariableName(variable, structure.fileExtension);
            int usageCount = countOccurrences(structure.originalCode, varName);
            structure.variableUsage.put(varName, usageCount);}
    }
    private static String extractVariableName(String variable, String extension) {
        switch (extension) {
            case ".java":
            case ".cpp":
                return variable.replaceAll("(?s).*?\\w+\\s+(\\w+)\\s*(=.*)?;.*", "$1").trim();
            case ".py":
                return variable.replaceAll("(?s)(\\w+)\\s*=.*", "$1").trim();
            case ".js":
                return variable.replaceAll("(?s)(let|const|var)\\s+(\\w+).*", "$2").trim();
            default:
                return "unknown";}
    }
    private static int countOccurrences(String code, String term) {
        int count = 0;
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(term) + "\\b");
        Matcher matcher = pattern.matcher(code);
        while (matcher.find()) {
            count++;}
        return count;}
    private static CodeStructure extractStructure(String code, String extension) {
        CodeStructure structure = new CodeStructure(code, extension);
        switch (extension) {
            case ".java":
                extractJavaStructure(structure);
                break;
            case ".py":
                extractPythonStructure(structure);
                break;
            case ".cpp":
                extractCppStructure(structure);
                break;
            case ".js":
                extractJsStructure(structure);
                break;}
        computeMethodLineCounts(structure);
        extractMethodDetails(structure);
        return structure;}
    private static void extractJavaStructure(CodeStructure structure) {
        Pattern importPattern = Pattern.compile("import\\s+.*?;");
        Matcher importMatcher = importPattern.matcher(structure.originalCode);
        while (importMatcher.find()) {
            structure.imports.add(importMatcher.group());}
        Pattern classPattern = Pattern.compile("class\\s+\\w+\\s*\\{[^}]*\\}", Pattern.DOTALL);
        Matcher classMatcher = classPattern.matcher(structure.originalCode);
        while (classMatcher.find()) {
            structure.classes.add(classMatcher.group());}
        Pattern methodPattern = Pattern.compile("(public|private|protected)?\\s*(static)?\\s*(\\w+)\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{[^}]*\\}", Pattern.DOTALL);
        Matcher methodMatcher = methodPattern.matcher(structure.originalCode);
        while (methodMatcher.find()) {
            structure.methods.add(methodMatcher.group());}
        Pattern variablePattern = Pattern.compile("(public|private|protected)?\\s*(static)?\\s*\\w+\\s+\\w+\\s*(=\\s*[^;]+)?;");
        Matcher variableMatcher = variablePattern.matcher(structure.originalCode);
        while (variableMatcher.find()) {
            structure.variables.add(variableMatcher.group());}
    }
    private static void extractPythonStructure(CodeStructure structure) {
        Pattern importPattern = Pattern.compile("^(import|from)\\s+.*$", Pattern.MULTILINE);
        Matcher importMatcher = importPattern.matcher(structure.originalCode);
        while (importMatcher.find()) {
            structure.imports.add(importMatcher.group());}
        Pattern classPattern = Pattern.compile("^class\\s+\\w+.*?:[\\s\\S]*?(?=^\\w|$)", Pattern.MULTILINE);
        Matcher classMatcher = classPattern.matcher(structure.originalCode);
        while (classMatcher.find()) {
            structure.classes.add(classMatcher.group());}
        Pattern methodPattern = Pattern.compile("^def\\s+(\\w+)\\s*\\([^)]*\\):[\\s\\S]*?(?=^\\w|$)", Pattern.MULTILINE);
        Matcher methodMatcher = methodPattern.matcher(structure.originalCode);
        while (methodMatcher.find()) {
            structure.methods.add(methodMatcher.group());}
        Pattern variablePattern = Pattern.compile("^\\w+\\s*=\\s*[^\\n]+", Pattern.MULTILINE);
        Matcher variableMatcher = variablePattern.matcher(structure.originalCode);
        while (variableMatcher.find()) {
            structure.variables.add(variableMatcher.group());}
    }
    private static void extractCppStructure(CodeStructure structure) {
        Pattern includePattern = Pattern.compile("#include\\s+[<\"][^>\"]+[>\"]");
        Matcher includeMatcher = includePattern.matcher(structure.originalCode);
        while (includeMatcher.find()) {
            structure.imports.add(includeMatcher.group()); }
        Pattern classPattern = Pattern.compile("(class|struct)\\s+\\w+\\s*\\{[^}]*\\};", Pattern.DOTALL);
        Matcher classMatcher = classPattern.matcher(structure.originalCode);
        while (classMatcher.find()) {
            structure.classes.add(classMatcher.group());
        }
        Pattern methodPattern = Pattern.compile("\\w+\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{[^}]*\\}", Pattern.DOTALL);
        Matcher methodMatcher = methodPattern.matcher(structure.originalCode);
        while (methodMatcher.find()) {
            structure.methods.add(methodMatcher.group());
        }
        Pattern variablePattern = Pattern.compile("\\w+\\s+\\w+\\s*(=\\s*[^;]+)?;");
        Matcher variableMatcher = variablePattern.matcher(structure.originalCode);
        while (variableMatcher.find()) {
            structure.variables.add(variableMatcher.group());}
    }
    private static void extractJsStructure(CodeStructure structure) {
        Pattern importPattern = Pattern.compile("(import|require\\s*\\([^)]+\\))\\s*;");
        Matcher importMatcher = importPattern.matcher(structure.originalCode);
        while (importMatcher.find()) {
            structure.imports.add(importMatcher.group());}
        Pattern classPattern = Pattern.compile("class\\s+\\w+\\s*\\{[^}]*\\}", Pattern.DOTALL);
        Matcher classMatcher = classPattern.matcher(structure.originalCode);
        while (classMatcher.find()) {
            structure.classes.add(classMatcher.group());}
        Pattern methodPattern = Pattern.compile("(function\\s+(\\w+)\\s*\\([^)]*\\)|(\\w+)\\s*\\([^)]*\\)\\s*=>\\s*\\{)[^}]*\\}", Pattern.DOTALL);
        Matcher methodMatcher = methodPattern.matcher(structure.originalCode);
        while (methodMatcher.find()) {
            structure.methods.add(methodMatcher.group());}
        Pattern variablePattern = Pattern.compile("(let|const|var)\\s+\\w+\\s*(=\\s*[^;]+)?;");
        Matcher variableMatcher = variablePattern.matcher(structure.originalCode);
        while (variableMatcher.find()) {
            structure.variables.add(variableMatcher.group());}
    }
    private static void computeMethodLineCounts(CodeStructure structure) {
        for (String method : structure.methods) {
            int lines = method.split("\n").length;
            String signature = extractMethodSignature(method, structure.fileExtension);
            structure.methodLineCounts.put(signature, lines);}
    }
    private static void extractMethodDetails(CodeStructure structure) {
        for (String method : structure.methods) {
            String signature = extractMethodSignature(method, structure.fileExtension);
            String params = extractMethodParameters(method, structure.fileExtension);
            String returnType = extractMethodReturnType(method, structure.fileExtension);
            structure.methodParameters.put(signature, params);
            structure.methodReturnTypes.put(signature, returnType);}
    }
    private static String extractMethodParameters(String method, String extension) {
        switch (extension) {
            case ".java":
            case ".cpp":
                String paramPart = method.replaceAll("(?s).*?\\([^)]*\\).*", "$0").replaceAll("(?s)\\{.*", "");
                return paramPart.replaceAll(".*?\\((.*?)\\).*", "$1").trim();
            case ".py":
                return method.replaceAll("(?s)def\\s+\\w+\\s*\\(([^)]*)\\):.*", "$1").trim();
            case ".js":
                return method.replaceAll("(?s)(function\\s+\\w+\\s*|\\w+\\s*)\\(([^)]*)\\).*", "$2").trim();
            default:
                return "unknown";}
    }
    private static String extractMethodReturnType(String method, String extension) {
        switch (extension) {
            case ".java":
                return method.replaceAll("(?s)(public|private|protected)?\\s*(static)?\\s*(\\w+)\\s+\\w+\\s*\\([^)]*\\).*", "$3").trim();
            case ".cpp":
                return method.replaceAll("(?s)(\\w+)\\s+\\w+\\s*\\([^)]*\\).*", "$1").trim();
            case ".py":
            case ".js":
                return "unknown";
            default:
                return "unknown";}
    }
    private static void filterStructure(CodeStructure structure, String filterType, String filterValue) {
        if (filterType.isEmpty() || filterType.equals("all")) {
            return;
        }
        switch (filterType) {
            case "imports":
                structure.classes.clear();
                structure.methods.clear();
                structure.variables.clear();
                break;
            case "classes":
                structure.imports.clear();
                structure.methods.clear();
                structure.variables.clear();
                break;
            case "methods":
                structure.imports.clear();
                structure.classes.clear();
                structure.variables.clear();
                if (!filterValue.isEmpty()) {
                    structure.methods.removeIf(method -> !method.contains(filterValue));}
                break;
            case "variables":
                structure.imports.clear();
                structure.classes.clear();
                structure.methods.clear();
                if (!filterValue.isEmpty()) {
                    structure.variables.removeIf(var -> !var.contains(filterValue));}
                break;
            case "method_name":
                structure.imports.clear();
                structure.classes.clear();
                structure.variables.clear();
                if (!filterValue.isEmpty()) {
                    structure.methods.removeIf(method -> !extractMethodName(method, structure.fileExtension).equals(filterValue));}
                break;
            case "variable_type":
                structure.imports.clear();
                structure.classes.clear();
                structure.methods.clear();
                if (!filterValue.isEmpty()) {
                    structure.variables.removeIf(var -> !extractVariableType(var, structure.fileExtension).equals(filterValue));
                }
                break;
            case "parameter_type":
                structure.imports.clear();
                structure.classes.clear();
                structure.variables.clear();
                if (!filterValue.isEmpty()) {
                    structure.methods.removeIf(method -> !structure.methodParameters.getOrDefault(extractMethodSignature(method, structure.fileExtension), "").contains(filterValue));}
                break;
            case "return_type":
                structure.imports.clear();
                structure.classes.clear();
                structure.variables.clear();
                if (!filterValue.isEmpty()) {
                    structure.methods.removeIf(method -> !structure.methodReturnTypes.getOrDefault(extractMethodSignature(method, structure.fileExtension), "").equals(filterValue));
                }
                break;}
    }
    private static String extractVariableType(String variable, String extension) {
        switch (extension) {
            case ".java":
            case ".cpp":
                return variable.replaceAll("(?s).*?(\\w+)\\s+\\w+\\s*(=.*)?;.*", "$1").trim();
            case ".py":
                return "unknown";
            case ".js":
                return variable.replaceAll("(?s)(let|const|var)\\s+\\w+.*", "$1").trim();
            default:
                return "unknown";}
    }
    private static String extractMethodName(String method, String extension) {
        switch (extension) {
            case ".java":
                return method.replaceAll("(?s).*?\\w+\\s+(\\w+)\\s*\\([^)]*\\).*", "$1").trim();
            case ".py":
                return method.replaceAll("(?s)def\\s+(\\w+)\\s*\\([^)]*\\):.*", "$1").trim();
            case ".cpp":
                return method.replaceAll("(?s).*?\\w+\\s+(\\w+)\\s*\\([^)]*\\).*", "$1").trim();
            case ".js":
                return method.replaceAll("(?s).*?(function\\s+|)(\\w+)\\s*\\([^)]*\\).*", "$2").trim();
            default:
                return "unknown";}
    }
    private static String restructureCode(CodeStructure structure) {
        StringBuilder restructured = new StringBuilder();
        if (!structure.imports.isEmpty()) {
            restructured.append(String.join("\n", sortByLength(structure.imports))).append("\n");
        }
        if (!structure.variables.isEmpty()) {
            restructured.append(String.join("\n", sortByLength(structure.variables))).append("\n");
        }
        if (!structure.classes.isEmpty()) {
            restructured.append(String.join("\n", sortByLength(structure.classes))).append("\n");
        }
        if (!structure.methods.isEmpty()) {
            restructured.append(String.join("\n", sortByLineCount(structure.methods, structure.methodLineCounts))).append("\n");
        }
        return restructured.toString().trim();
    }
    private static List<String> sortByLength(List<String> elements) {
        List<String> sorted = new ArrayList<>(elements);
        sorted.sort((a, b) -> b.length() - a.length());
        return sorted;}
    private static List<String> sortByLineCount(List<String> elements, Map<String, Integer> lineCounts) {
        List<String> sorted = new ArrayList<>(elements);
        sorted.sort((a, b) -> {
            String sigA = normalizeCode(a);
            String sigB = normalizeCode(b);
            return lineCounts.getOrDefault(sigB, 0) - lineCounts.getOrDefault(sigA, 0);
        });
        return sorted;}
    private static String normalizeCode(String code) {
        return code.replaceAll("\\s+", " ").trim();
    }
    private static String formatIndentation(String code, String extension) {
        StringBuilder formatted = new StringBuilder();
        String[] lines = code.split("\n");
        int indentLevel = 0;
        String indentUnit = extension.equals(".py") ? "    " : "\t";
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.endsWith("}") || trimmed.endsWith(":")) {
                indentLevel = Math.max(0, indentLevel - 1);
            }
            formatted.append(indentUnit.repeat(indentLevel)).append(trimmed).append("\n");
            if (trimmed.endsWith("{") || (extension.equals(".py") && trimmed.endsWith(":"))) {
                indentLevel++;
            }
        }
        return formatted.toString().trim();}
    private static String transformCase(String code, String transformCase) {
        if (transformCase.equals("uppercase")) {
            return transformIdentifiers(code, true);
        } else if (transformCase.equals("lowercase")) {
            return transformIdentifiers(code, false);
        }
        return code;}
    private static String transformIdentifiers(String code, boolean toUpper) {
        Pattern identifierPattern = Pattern.compile("\\b\\w+\\b");
        Matcher matcher = identifierPattern.matcher(code);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            result.append(code, lastEnd, matcher.start());
            String identifier = matcher.group();
            result.append(toUpper ? identifier.toUpperCase() : identifier.toLowerCase());
            lastEnd = matcher.end();}
        result.append(code.substring(lastEnd));
        return result.toString();}
    private static String editCode(String code, String extension, String replaceFrom, String replaceTo, boolean formatIndent, String transformCase) {
        String edited = code;
        if (!replaceFrom.isEmpty()) {
            edited = edited.replaceAll(Pattern.quote(replaceFrom), replaceTo);
        }
        switch (extension) {
            case ".java":
                edited = addJavaDocComments(edited);
                break;
            case ".py":
                edited = addPythonDocstrings(edited);
                break;
            case ".cpp":
                edited = addCppComments(edited);
                break;
            case ".js":
                edited = addJsComments(edited);
                break;}
        if (formatIndent) {
            edited = formatIndentation(edited, extension);
        }
        edited = transformCase(edited, transformCase);
        return edited;}
    private static String addJavaDocComments(String code) {
        StringBuilder result = new StringBuilder();
        Pattern methodPattern = Pattern.compile("(public|private|protected)?\\s*(static)?\\s*\\w+\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{");
        Matcher matcher = methodPattern.matcher(code);
        int lastEnd = 0;
        while (matcher.find()) {
            result.append(code, lastEnd, matcher.start());
            String methodName = matcher.group(3);
            result.append("/**").append("\n")
                  .append(" * Method: ").append(methodName).append("\n")
                  .append(" * Description: Auto-generated method documentation").append("\n")
                  .append(" * Parameters: Auto-detected").append("\n")
                  .append(" * Returns: Auto-detected").append("\n")
                  .append(" */**").append("\n");
            result.append(matcher.group());
            lastEnd = matcher.end(); }
        result.append(code.substring(lastEnd));
        return result.toString();}
    private static String addPythonDocstrings(String code) {
        StringBuilder result = new StringBuilder();
        Pattern methodPattern = Pattern.compile("^def\\s+(\\w+)\\s*\\([^)]*\\):", Pattern.MULTILINE);
        Matcher matcher = methodPattern.matcher(code);
        int lastEnd = 0;
        while (matcher.find()) {
            result.append(code, lastEnd, matcher.start());
            String methodName = matcher.group(1);
            result.append("\"\"\"").append("\n")
                  .append("Function: ").append(methodName).append("\n")
                  .append("Description: Auto-generated function documentation").append("\n")
                  .append("Args: Auto-detected").append("\n")
                  .append("Returns: Auto-detected").append("\n")
                  .append("\"\"\"").append("\n");
            result.append(matcher.group());
            lastEnd = matcher.end();}
        result.append(code.substring(lastEnd));
        return result.toString();
    }
    private static String addCppComments(String code) {
        StringBuilder result = new StringBuilder();
        Pattern methodPattern = Pattern.compile("\\w+\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{");
        Matcher matcher = methodPattern.matcher(code);
        int lastEnd = 0;
        while (matcher.find()) {
            result.append(code, lastEnd, matcher.start());
            String methodName = matcher.group(1);
            result.append("// ").append(methodName).append(" - Auto-generated documentation").append("\n")
                  .append("// Parameters: Auto-detected").append("\n")
                  .append("// Returns: Auto-detected").append("\n");
            result.append(matcher.group());
            lastEnd = matcher.end();
        }
        result.append(code.substring(lastEnd));
        return result.toString();}
    private static String addJsComments(String code) {
        StringBuilder result = new StringBuilder();
        Pattern methodPattern = Pattern.compile("(function\\s+(\\w+)\\s*\\([^)]*\\)|(\\w+)\\s*\\([^)]*\\)\\s*=>\\s*\\{)");
        Matcher matcher = methodPattern.matcher(code);
        int lastEnd = 0;
        while (matcher.find()) {
            result.append(code, lastEnd, matcher.start());
            String methodName = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            result.append("// ").append(methodName).append(" - Auto-generated documentation").append("\n")
                  .append("// Parameters: Auto-detected").append("\n")
                  .append("// Returns: Auto-detected").append("\n");
            result.append(matcher.group());
            lastEnd = matcher.end();
        }
        result.append(code.substring(lastEnd));
        return result.toString();
    }
    private static String generateFileReport(File file, CodeStructure structure) {
        StringBuilder report = new StringBuilder();
        report.append("File: ").append(file.getName()).append("\n");
        report.append("Extension: ").append(structure.fileExtension).append("\n");
        report.append("Total Lines: ").append(structure.lineCount).append("\n");
        report.append("Non-blank Lines: ").append(countNonBlankLines(structure.originalCode)).append("\n");
        report.append("Complexity: ").append(analyzeCodeComplexity(structure.originalCode)).append("\n");
        report.append("Imports: ").append(structure.imports.size()).append("\n");
        report.append("Classes: ").append(structure.classes.size()).append("\n");
        report.append("Methods: ").append(structure.methods.size()).append("\n");
        report.append("Variables: ").append(structure.variables.size()).append("\n");
        report.append("Method Details:\n");
        for (String method : structure.methods) {
            String signature = structure.methodSignatures.getOrDefault(method, "Unknown");
            int lines = structure.methodLineCounts.getOrDefault(signature, 0);
            String params = structure.methodParameters.getOrDefault(signature, "None");
            String returnType = structure.methodReturnTypes.getOrDefault(signature, "Unknown");
            report.append("  - ").append(signature).append("\n")
                  .append("    Lines: ").append(lines).append("\n")
                  .append("    Parameters: ").append(params).append("\n")
                  .append("    Return Type: ").append(returnType).append("\n");
        }
        report.append("Variable Usage:\n");
        for (Map.Entry<String, Integer> entry : structure.variableUsage.entrySet()) {
            report.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" uses\n");
        }
        report.append("----------------------------------------\n");
        return report.toString();
    }
    private static void saveReport(String report) throws IOException {
        String reportPath = Paths.get(OUTPUT_DIR, REPORT_FILE).toString();
        Files.write(Paths.get(reportPath), report.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    private static void saveIndividualFiles(List<CodeStructure> structures, String replaceFrom, String replaceTo, boolean formatIndent, String transformCase) throws IOException {
        for (CodeStructure structure : structures) {
            String restructured = restructureCode(structure);
            String edited = editCode(restructured, structure.fileExtension, replaceFrom, replaceTo, formatIndent, transformCase);
            String outputPath = Paths.get(OUTPUT_DIR, "processed_" + UUID.randomUUID() + structure.fileExtension).toString();
            Files.write(Paths.get(outputPath), edited.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Saved: " + outputPath);
        }
    }
    private static void concatenateFiles(List<CodeStructure> structures, String replaceFrom, String replaceTo, boolean formatIndent, String transformCase) throws IOException {
        StringBuilder concatenated = new StringBuilder();
        for (CodeStructure structure : structures) {
            String restructured = restructureCode(structure);
            String edited = editCode(restructured, structure.fileExtension, replaceFrom, replaceTo, formatIndent, transformCase);
            concatenated.append("// File: ").append(structure.fileExtension).append("\n");
            concatenated.append(edited).append("\n\n");
        }
        String outputPath = Paths.get(OUTPUT_DIR, "concatenated_output.txt").toString();
        Files.write(Paths.get(outputPath), concatenated.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Saved concatenated file: " + outputPath);
    }
    private static void validateInputPath(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new IOException("Path does not exist: " + path);
        }
        if (!file.isDirectory()) {
            throw new IOException("Path is not a directory: " + path);}
    }
    private static boolean isValidRegex(String regex) {
        try {
            Pattern.compile(regex);
            return true;
        } catch (PatternSyntaxException e) {
            return false;}
    }
    private static int countNonBlankLines(String code) {
        String[] lines = code.split("\n");
        int count = 0;
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                count++;}
        }
        return count;}
    private static String analyzeCodeComplexity(String code) {
        int cyclomaticComplexity = 1;
        Pattern controlFlowPattern = Pattern.compile("\\b(if|else|while|for|switch|case|try|catch)\\b");
        Matcher matcher = controlFlowPattern.matcher(code);
        while (matcher.find()) {
            cyclomaticComplexity++;}
        return "Cyclomatic Complexity: " + cyclomaticComplexity;
    }
    private static void logProcessingStart(File file) {
        System.out.println("Starting processing for: " + file.getName() + " at " + new Date());
    }
    private static void logProcessingEnd(File file) {
        System.out.println("Finished processing: " + file.getName() + " at " + new Date());
    }
    private static void logError(String errorMessage) {
        try {
            String errorLog = Paths.get(OUTPUT_DIR, "error_log.txt").toString();
            Files.write(Paths.get(errorLog), (new Date() + ": " + errorMessage + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Failed to log error: " + e.getMessage());}
    }
    private static String reformatCodePatterns(String code, String extension) {
        switch (extension) {
            case ".java":
                return reformatJavaPatterns(code);
            case ".py":
                return reformatPythonPatterns(code);
            case ".cpp":
                return reformatCppPatterns(code);
            case ".js":
                return reformatJsPatterns(code);
            default:
                return code;}
    }
    private static String reformatJavaPatterns(String code) {
        String reformatted = code.replaceAll("System\\.out\\.println\\(", "System.out.print(");
        return reformatted;}
    private static String reformatPythonPatterns(String code) {
        String reformatted = code.replaceAll("print\\(", "print(");
        return reformatted;}
    private static String reformatCppPatterns(String code) {
        String reformatted = code.replaceAll("std::cout\\s*<<", "std::cout << ");
        return reformatted;}
    private static String reformatJsPatterns(String code) {
        String reformatted = code.replaceAll("console\\.log\\(", "console.log(");
        return reformatted;}
    private static String validateCodeStructure(String code, String extension) {
        switch (extension) {
            case ".java":
                return validateJavaStructure(code);
            case ".py":
                return validatePythonStructure(code);
            case ".cpp":
                return validateCppStructure(code);
            case ".js":
                return validateJsStructure(code);
            default:
                return code;}
    }
    private static String validateJavaStructure(String code) {
        if (!code.contains("{")) {
            logError("Invalid Java structure: Missing opening brace");
            return code;}
        if (!code.contains("}")) {
            logError("Invalid Java structure: Missing closing brace");
            return code;}
        return code;
    }
    private static String validatePythonStructure(String code) {
        Pattern defPattern = Pattern.compile("^def\\s+\\w+\\s*\\([^)]*\\):", Pattern.MULTILINE);
        Matcher matcher = defPattern.matcher(code);
        while (matcher.find()) {
            String defLine = matcher.group();
            if (!defLine.endsWith(":")) {
                logError("Invalid Python structure: Function definition missing colon");
            }
        }
        return code;}
    private static String validateCppStructure(String code) {
        if (!code.contains("{")) {
            logError("Invalid C++ structure: Missing opening brace");
            return code;}
        if (!code.contains("}")) {
            logError("Invalid C++ structure: Missing closing brace");
            return code;}
        return code;
    }
    private static String validateJsStructure(String code) {
        Pattern funcPattern = Pattern.compile("(function\\s+\\w+\\s*\\([^)]*\\)|\\w+\\s*\\([^)]*\\)\\s*=>\\s*\\{)");
        Matcher matcher = funcPattern.matcher(code);
        while (matcher.find()) {
            String funcLine = matcher.group();
            if (!funcLine.contains("(") || !funcLine.contains(")")) {
                logError("Invalid JavaScript structure: Function definition missing parentheses");}
        }
        return code;}
    private static String generateCodeMetrics(CodeStructure structure) {
        StringBuilder metrics = new StringBuilder();
        metrics.append("Code Metrics for ").append(structure.fileExtension).append("\n");
        metrics.append("Total Lines: ").append(structure.lineCount).append("\n");
        metrics.append("Non-blank Lines: ").append(countNonBlankLines(structure.originalCode)).append("\n");
        metrics.append("Complexity: ").append(analyzeCodeComplexity(structure.originalCode)).append("\n");
        metrics.append("Variable Usage Frequency:\n");
        for (Map.Entry<String, Integer> entry : structure.variableUsage.entrySet()) {
            metrics.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" uses\n");
        }
        return metrics.toString();
    }
}