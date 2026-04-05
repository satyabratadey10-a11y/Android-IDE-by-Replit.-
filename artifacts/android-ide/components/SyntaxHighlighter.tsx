import React, { useMemo } from "react";
import { ScrollView, StyleSheet, Text, View } from "react-native";
import colors from "@/constants/colors";

interface Token {
  text: string;
  type:
    | "keyword"
    | "string"
    | "comment"
    | "number"
    | "type"
    | "function"
    | "operator"
    | "default";
}

const KOTLIN_KEYWORDS = new Set([
  "fun","val","var","class","object","interface","when","if","else","for","while",
  "return","import","package","override","private","public","protected","internal",
  "data","sealed","abstract","open","companion","by","is","as","in","out","null",
  "true","false","this","super","it","let","run","apply","also","with","suspend",
  "coroutine","launch","async","flow","emit","collect","init","constructor",
]);

const JAVA_KEYWORDS = new Set([
  "public","private","protected","static","final","class","interface","extends",
  "implements","new","void","return","if","else","for","while","do","switch","case",
  "break","continue","try","catch","finally","throw","throws","import","package",
  "abstract","boolean","int","float","double","long","char","byte","short",
  "null","true","false","this","super","instanceof","enum","default","synchronized",
]);

const CPP_KEYWORDS = new Set([
  "int","float","double","char","bool","void","return","if","else","for","while",
  "do","switch","case","break","continue","class","struct","namespace","using",
  "include","define","ifndef","endif","public","private","protected","virtual",
  "override","const","static","new","delete","nullptr","true","false","auto",
  "template","typename","typedef","enum","union","extern","inline","explicit",
]);

const XML_TAGS = new Set([
  "manifest","application","activity","intent-filter","action","category","uses-permission",
  "service","receiver","provider","data","meta-data","layout","LinearLayout",
  "RelativeLayout","ConstraintLayout","TextView","Button","EditText","ImageView",
  "RecyclerView","Fragment","include","merge",
]);

function tokenizeKotlin(code: string): Token[][] {
  return tokenizeGeneric(code, KOTLIN_KEYWORDS);
}

function tokenizeJava(code: string): Token[][] {
  return tokenizeGeneric(code, JAVA_KEYWORDS);
}

function tokenizeCpp(code: string): Token[][] {
  return tokenizeGeneric(code, CPP_KEYWORDS);
}

function tokenizeGeneric(code: string, keywords: Set<string>): Token[][] {
  const lines = code.split("\n");
  return lines.map((line) => {
    const tokens: Token[] = [];
    let i = 0;
    while (i < line.length) {
      if (line[i] === "/" && line[i + 1] === "/") {
        tokens.push({ text: line.slice(i), type: "comment" });
        break;
      }
      if (line[i] === '"' || line[i] === "'") {
        const q = line[i];
        let j = i + 1;
        while (j < line.length && line[j] !== q) j++;
        tokens.push({ text: line.slice(i, j + 1), type: "string" });
        i = j + 1;
        continue;
      }
      if (/\d/.test(line[i])) {
        let j = i;
        while (j < line.length && /[\d.fL]/.test(line[j])) j++;
        tokens.push({ text: line.slice(i, j), type: "number" });
        i = j;
        continue;
      }
      if (/[a-zA-Z_]/.test(line[i])) {
        let j = i;
        while (j < line.length && /\w/.test(line[j])) j++;
        const word = line.slice(i, j);
        const nextChar = line[j];
        if (keywords.has(word)) {
          tokens.push({ text: word, type: "keyword" });
        } else if (nextChar === "(") {
          tokens.push({ text: word, type: "function" });
        } else if (/[A-Z]/.test(word[0])) {
          tokens.push({ text: word, type: "type" });
        } else {
          tokens.push({ text: word, type: "default" });
        }
        i = j;
        continue;
      }
      if (/[+\-*/<>=!&|^~%]/.test(line[i])) {
        tokens.push({ text: line[i], type: "operator" });
        i++;
        continue;
      }
      tokens.push({ text: line[i], type: "default" });
      i++;
    }
    return tokens;
  });
}

function tokenizeXml(code: string): Token[][] {
  const lines = code.split("\n");
  return lines.map((line) => {
    const tokens: Token[] = [];
    let i = 0;
    while (i < line.length) {
      if (line[i] === "<") {
        tokens.push({ text: "<", type: "operator" });
        i++;
        let j = i;
        while (j < line.length && /\S/.test(line[j]) && line[j] !== ">") j++;
        const tag = line.slice(i, j);
        tokens.push({ text: tag, type: "keyword" });
        i = j;
        continue;
      }
      if (line[i] === '"') {
        let j = i + 1;
        while (j < line.length && line[j] !== '"') j++;
        tokens.push({ text: line.slice(i, j + 1), type: "string" });
        i = j + 1;
        continue;
      }
      if (line[i] === "=") {
        tokens.push({ text: "=", type: "operator" });
        i++;
        continue;
      }
      if (/[a-zA-Z_:@]/.test(line[i])) {
        let j = i;
        while (j < line.length && /[\w:@.]/.test(line[j])) j++;
        const word = line.slice(i, j);
        tokens.push({ text: word, type: "type" });
        i = j;
        continue;
      }
      tokens.push({ text: line[i], type: "default" });
      i++;
    }
    return tokens;
  });
}

function tokenizeGradle(code: string): Token[][] {
  const keywords = new Set([
    "plugins","android","dependencies","apply","plugin","implementation","testImplementation",
    "androidTestImplementation","buildTypes","release","debug","defaultConfig","compileOptions",
    "kotlinOptions","buildFeatures","composeOptions","namespace","compileSdk","targetSdk",
    "minSdk","versionCode","versionName","alias","classpath","repositories","allprojects",
    "google","mavenCentral","id","version","true","false",
  ]);
  return tokenizeGeneric(code, keywords);
}

function getTokenizer(language?: string) {
  switch (language) {
    case "kotlin": return tokenizeKotlin;
    case "java": return tokenizeJava;
    case "cpp":
    case "c": return tokenizeCpp;
    case "xml": return tokenizeXml;
    case "gradle": return tokenizeGradle;
    default: return (code: string) => code.split("\n").map((l) => [{ text: l, type: "default" as const }]);
  }
}

function tokenColor(type: Token["type"]): string {
  switch (type) {
    case "keyword": return colors.light.codeKeyword;
    case "string": return colors.light.codeString;
    case "comment": return colors.light.codeComment;
    case "number": return colors.light.codeNumber;
    case "type": return colors.light.codeType;
    case "function": return colors.light.codeFunction;
    case "operator": return colors.light.codeOperator;
    default: return colors.light.foreground;
  }
}

interface Props {
  code: string;
  language?: string;
  showLineNumbers?: boolean;
  breakpointLines?: number[];
  onToggleBreakpoint?: (line: number) => void;
}

export default function SyntaxHighlighter({
  code,
  language,
  showLineNumbers = true,
  breakpointLines = [],
  onToggleBreakpoint,
}: Props) {
  const tokenized = useMemo(() => {
    const tokenize = getTokenizer(language);
    return tokenize(code);
  }, [code, language]);

  return (
    <ScrollView horizontal style={styles.scroll} showsHorizontalScrollIndicator={false}>
      <ScrollView style={styles.inner} showsVerticalScrollIndicator={false}>
        {tokenized.map((lineTokens, lineIdx) => {
          const lineNum = lineIdx + 1;
          const hasBreakpoint = breakpointLines.includes(lineNum);
          return (
            <View key={lineIdx} style={styles.lineRow}>
              {showLineNumbers && (
                <Text
                  style={[styles.lineNum, hasBreakpoint && styles.lineNumBreakpoint]}
                  onPress={() => onToggleBreakpoint?.(lineNum)}
                >
                  {hasBreakpoint ? "●" : String(lineNum).padStart(3, " ")}
                </Text>
              )}
              <Text style={styles.lineContent}>
                {lineTokens.map((token, ti) => (
                  <Text key={ti} style={{ color: tokenColor(token.type) }}>
                    {token.text}
                  </Text>
                ))}
              </Text>
            </View>
          );
        })}
      </ScrollView>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scroll: { flex: 1 },
  inner: { flex: 1, padding: 8 },
  lineRow: {
    flexDirection: "row",
    minHeight: 20,
  },
  lineNum: {
    width: 36,
    color: colors.light.codeComment,
    fontFamily: "monospace",
    fontSize: 12,
    textAlign: "right",
    paddingRight: 8,
    userSelect: "none" as any,
  },
  lineNumBreakpoint: {
    color: colors.light.destructive,
  },
  lineContent: {
    fontFamily: "monospace",
    fontSize: 13,
    lineHeight: 20,
    color: colors.light.foreground,
  },
});
