<h1>PurityChecker</h1>


Table of Contents
=================

   * [PurityChecker](#puritychecker)
   * [RefactoringMiner](#refactoringminer)
   * [Current precision and recall](#current-precision-and-recall)
   * [How to use PurityChecker](#how-to-use-puritychecker)
   * [PurityChecker Applications](#puritychecker-applications)
   * [RefactoringMiner API usage guidelines](#api-usage-guidelines)
      * [With a locally cloned git repository](#with-a-locally-cloned-git-repository)
      * [With two directories containing Java source code](#with-two-directories-containing-java-source-code)
      * [With file contents as strings](#with-file-contents-as-strings)
      * [With all information fetched directly from GitHub](#with-all-information-fetched-directly-from-github)
      * [With a GitHub pull request](#with-a-github-pull-request)

# PurityChecker 
<div align="justify">PurityChecker is a tool for detecting purity of method-level refactorings in Java. The tool is build on top of RefactoringMiner, which is a refactoring detection tool.
<br/>
<br/>

A method-level refactoring is considered **pure** if it does not include any modifications that alter the behavior of the refactored code (method bodies).
<br/>

There could be multiple scenarios in which a code change, beyond what is considered as specific refactoring type mechanics, does not change the behavior of the refactored code. As the main novelty of this tool, PurityChecker is the first tool that is able to capture the changes caused by the application of overlapped refactorings.

For example, in the following figure, the `setFrameCentered` method, which is labeled as "1", has been inlined into the `placeWindow` method. As we know, the Inline Method can be summarized as simply replacing the function body with function calls. But, within this example, two statements have been removed from the inlined operation. This statement removal is attributed to the two **overlapping Inline Variable refactorings** which are conducted on top of the Inline Method refactoring. As we can see, this change **does not change the behavior of the refactorin**g, although this is not part of the inline method refactoring mechanics. So, this refactoring is considered as a **pure Inline Method refactoring** operation, as there is no code modification involved which changes the refactoring behavior.
</div>

<img width="695" alt="image" src="https://github.com/pedramnoori/RefactoringMiner/assets/37044356/f7b42519-5985-45ac-ad5b-e90c258f430f">

<br>
<br>

PurityChecker utilizes RefactoringMiner's resources by initially executing the RefactoringMiner tool on a commit. Therefore, when applied to a commit, PurityChecker offers a **list of refactorings** undertaken within a code change provided by RefactoringMiner, accompanied by **purity details specific to method-level refactorings**.

# RefactoringMiner
RefactoringMiner is a library/API written in Java that can detect refactorings applied in the history of a Java project.

At present, the tool identifies a broad spectrum of 98 distinct refactoring types across various categories, encompassing method-level, class-level, variable-related, test-related, and more.


# Current precision and recall
As of **November 8, 2023** the precision and recall of the tool on **two** training and testing oracles consisting of **over 2400 method-level refactorings** is:

<h2>Training Oracle:</h2>

| Refactoring Type | Precision | Recall | Specificity | # Validated Cases |
|:-----------------------|-------------:|-----------:|-----------:|--------:|
|**Total (W. Average)**|**97.07**  |  **90.01**  | **97**  | **1912**  |
|Extract Method|95.28  |  88.68  | 95.31  | 925  |
|Extract and Move Method|92.31  |  73.47  | 93.34  | 94  |
|Inline Method|98.28  |  95  | 97.73  | 104  |
|Move and Inline Method|100  |  100  | 100  | 13  |
|Move Method|100  |  92.03  | 100  | 345  |
|Move and Rename Method|100  |  80.65  | 100  | 107  |
|Pull Up Method|100  |  94.92  | 100  | 282  |
|Push Down Method|100  |  87.18  | 100  | 42  |

<h2>Testing Oracle:</h2>

| Refactoring Type | Precision | Recall | Specificity | # Validated Cases |
|:-----------------------|-------------:|-----------:|-----------:|--------:|
|**Total (W. Average)**|**98.57**  |  **87.61**  | **98.34**  | **452**  |
|Extract Method|97.73  |  81.13  | 97.5  | 93  |
|Extract and Move Method|100  |  93.02  | 100  | 51  |
|Inline Method|96.97  |  86.49  | 98.21  | 93  |
|Move and Inline Method|100  |  100  | 100  | 6  |
|Move Method|100  |  82.81  | 100  | 77  |
|Move and Rename Method|100  |  75  | 100  | 26  |
|Pull Up Method|100  |  86.36  | 100  | 53  |
|Push Down Method|100  |  100  | 100  | 53  |


# How to use PurityChecker

`pedram`


PurityChecker features a crucial method called `isPure`. This method takes two main arguments: `umlModelDiff`, which contains information about the parent and child classes along with all the changes in a commit, and `refactorings`, which includes a list of refactorings performed in the given commit. These arguments are obtained by invoking RefactoringMiner on a specific commit (commit URL).
<br>
<div align="justify">
PurityChecker integrates with all RefactoringMiner APIs. RefactoringMiner offers multiple APIs for detecting refactorings, allowing users to work with locally cloned git repositories, directories containing Java source code, file contents as strings, and directly through the GitHub API. PurityChecker’s compatibility lies in its ability to utilize the same arguments as specified earlier, ensuring that it functions in tandem with all RefactoringMiner API options.
<br>
<br>
To enhance the usability of PurityChecker, we have developed an API method within the “API.java” file. This API method simplifies the process by only requiring the commit URL as input and providing the purity output as a result.
<br>
<br>
The output of PurityChecker indicates whether the refactorings are pure or not, along with an automatically generated comment that explains why the changes are behavior-preserving or not. For example, in the case of an Extract Method refactoring detected as pure due to the application of an overlapping Inline Variable refactoring, PurityChecker generates a comment like this: “Overlapped refactoring - can be identical by undoing the overlapped refactoring - Inline Variable,” and assigns a purity value of true.
</div>

# PurityChecker Applications

## Code Review
Code reviewer point of view, having the knowledge about purity of refactorings gives the reviewer a deeper insight into the code modifications. For instance, the reviewer can simply skip the changes happening within pure refactorings, as she can be sure that there is no functionality change involved within those cases.



# API usage guidelines
## With a locally cloned git repository
RefactoringMiner can automatically detect refactorings in the entire history of 
git repositories, between specified commits or tags, or at specified commits.

In the code snippet below we demonstrate how to print all refactorings performed
in the toy project https://github.com/danilofes/refactoring-toy-example.git.

```java
GitService gitService = new GitServiceImpl();
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

Repository repo = gitService.cloneIfNotExists(
    "tmp/refactoring-toy-example",
    "https://github.com/danilofes/refactoring-toy-example.git");

miner.detectAll(repo, "master", new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

You can also analyze between commits using `detectBetweenCommits` or between tags using `detectBetweenTags`. RefactoringMiner will iterate through all *non-merge* commits from **start** commit/tag to **end** commit/tag.

```java
// start commit: 819b202bfb09d4142dece04d4039f1708735019b
// end commit: d4bce13a443cf12da40a77c16c1e591f4f985b47
miner.detectBetweenCommits(repo, 
    "819b202bfb09d4142dece04d4039f1708735019b", "d4bce13a443cf12da40a77c16c1e591f4f985b47",
    new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

```java
// start tag: 1.0
// end tag: 1.1
miner.detectBetweenTags(repo, "1.0", "1.1", new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

It is possible to analyze a specifc commit using `detectAtCommit` instead of `detectAll`. The commit
is identified by its SHA key, such as in the example below:

```java
miner.detectAtCommit(repo, "05c1e773878bbacae64112f70964f4f2f7944398", new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

## With two directories containing Java source code

It is possible to detect refactorings between the Java files in two directories
containing the code before and after some changes.
This feature supports the detection of renamed and moved classes,
and automatically excludes from the analysis any files with identical contents:  

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
// You must provide absolute paths to the directories. Relative paths will cause exceptions.
File dir1 = new File("/home/user/tmp/v1");
File dir2 = new File("/home/user/tmp/v2");
miner.detectAtDirectories(dir1, dir2, new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
// You must provide absolute paths to the directories. Relative paths will cause exceptions.
Path dir1 = Paths.get("/home/user/tmp/v1");
Path dir1 = Paths.get("/home/user/tmp/v2");
miner.detectAtDirectories(dir1, dir2, new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

## With file contents as strings

You can provide two maps (before and after the changes) where the keys are file paths, and the values are the corresponding file contents.
Each key should correspond to a file path starting from the root of the repository. For example, `src/org/refactoringminer/api/GitHistoryRefactoringMiner.java`.

After populating the maps, you can use the following code snippet:

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
// Each key should correspond to a file path starting from the root of the repository
Map<String, String> fileContentsBefore;
Map<String, String> fileContentsAfter;
// populate the maps
miner.detectAtFileContents(fileContentsBefore, fileContentsAfter, new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

## With all information fetched directly from GitHub

To use this API, please provide a valid OAuth token in the `github-oauth.properties` file.
You can generate an OAuth token in GitHub `Settings` -> `Developer settings` -> `Personal access tokens`.

If you don't want to clone locally the repository, you can use the following code snippet:

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
miner.detectAtCommit("https://github.com/danilofes/refactoring-toy-example.git",
    "36287f7c3b09eff78395267a3ac0d7da067863fd", new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
}, 10);
```

## With a GitHub pull request

To use this API, please provide a valid OAuth token in the `github-oauth.properties` file.
You can generate an OAuth token in GitHub `Settings` -> `Developer settings` -> `Personal access tokens`.

If you want to analyze all commits of a pull request, you can use the following code snippet:

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
miner.detectAtPullRequest("https://github.com/apache/drill.git", 1807, new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
}, 10);
```

# AST Diff API usage guidelines

RefactoringMiner is actually the only tool that generates AST diff at commit level, supports multi-mappings (one-to-many, many-to-one, many-to-many mappings), matches AST nodes of different AST types, and supports semantic diff in a fully refactoring-aware fashion.
You can explore its advanced AST diff capabilities in our [AST Diff Gallery](https://github.com/tsantalis/RefactoringMiner/wiki/AST-Diff-Gallery).

All AST Diff APIs return a `Set<ASTDiff>`, where each [ASTDiff](https://github.com/tsantalis/RefactoringMiner/blob/master/src/org/refactoringminer/astDiff/actions/ASTDiff.java) object corresponds to a pair of Java Compilation Units.

`ASTDiff` extends `com.github.gumtreediff.actions.Diff` and thus it is compatible with the [GumTree](https://github.com/GumTreeDiff/gumtree) core APIs.

More detailed documentation can be found in [GitHistoryRefactoringMiner](https://github.com/tsantalis/RefactoringMiner/blob/master/src/org/refactoringminer/api/GitHistoryRefactoringMiner.java) JavaDoc.

```java
// With a locally cloned git repository
GitService gitService = new GitServiceImpl();
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

Repository repo = gitService.cloneIfNotExists(
    "tmp/refactoring-toy-example",
    "https://github.com/danilofes/refactoring-toy-example.git");

Set<ASTDiff> diffs = miner.diffAtCommit(repo, "36287f7c3b09eff78395267a3ac0d7da067863fd");
```

To use the following API, please provide a valid OAuth token in the `github-oauth.properties` file.
You can generate an OAuth token in GitHub `Settings` -> `Developer settings` -> `Personal access tokens`.
```java
// With all information fetched directly from GitHub
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
Set<ASTDiff> diffs = miner.diffAtCommit("https://github.com/danilofes/refactoring-toy-example.git",
    "36287f7c3b09eff78395267a3ac0d7da067863fd", 10);
```

```java
// With two directories containing Java source code (File API)
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
// You must provide absolute paths to the directories. Relative paths will cause exceptions.
File dir1 = new File("/home/user/tmp/v1");
File dir2 = new File("/home/user/tmp/v2");
Set<ASTDiff> diffs = miner.diffAtDirectories(dir1, dir2);
```

```java
// With two directories containing Java source code (Path API)
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
// You must provide absolute paths to the directories. Relative paths will cause exceptions.
Path dir1 = Paths.get("/home/user/tmp/v1");
Path dir1 = Paths.get("/home/user/tmp/v2");
Set<ASTDiff> diffs = miner.diffAtDirectories(dir1, dir2);
```

To **visualize** the diff, you can use our [DiffBenchmark project](https://github.com/pouryafard75/DiffBenchmark).

# Location information for the detected refactorings
All classes implementing the `Refactoring` interface include refactoring-specific location information.
For example, `ExtractOperationRefactoring` offers the following methods:

1. `getSourceOperationCodeRangeBeforeExtraction()` : Returns the code range of the source method in the **parent** commit
2. `getSourceOperationCodeRangeAfterExtraction()` : Returns the code range of the source method in the **child** commit
3. `getExtractedOperationCodeRange()` : Returns the code range of the extracted method in the **child** commit
4. `getExtractedCodeRangeFromSourceOperation()` : Returns the code range of the extracted code fragment from the source method in the **parent** commit
5. `getExtractedCodeRangeToExtractedOperation()` : Returns the code range of the extracted code fragment to the extracted method in the **child** commit
6. `getExtractedOperationInvocationCodeRange()` : Returns the code range of the invocation to the extracted method inside the source method in the **child** commit

Each method returns a `CodeRange` object including the following properties:
```java
String filePath
int startLine
int endLine
int startColumn
int endColumn
```
Alternatively, you can use the methods `List<CodeRange> leftSide()` and `List<CodeRange> rightSide()` to get a list of `CodeRange` objects for the left side (i.e., **parent** commit) and right side (i.e., **child** commit) of the refactoring, respectively.

# Statement matching information for the detected refactorings
All method-related refactoring (Extract/Inline/Move/Rename/ExtractAndMove Operation) objects come with a `UMLOperationBodyMapper` object, which can be obtained by calling method `getBodyMapper()` on the refactoring object.

Let's consider the Extract Method refactoring in commit [JetBrains/intellij-community@7ed3f27](https://github.com/JetBrains/intellij-community/commit/7ed3f273ab0caf0337c22f0b721d51829bb0c877)

![example|1665x820](https://user-images.githubusercontent.com/1483516/52974463-b0240000-338f-11e9-91e2-966f20be2514.png)

#1. You can use the following code snippet to obtain the **newly added statements** in the extracted method:
```java
ExtractOperationRefactoring refactoring = ...;
UMLOperationBodyMapper mapper = refactoring.getBodyMapper();
List<StatementObject> newLeaves = mapper.getNonMappedLeavesT2(); //newly added leaf statements
List<CompositeStatementObject> newComposites = mapper.getNonMappedInnerNodesT2(); //newly added composite statements
List<StatementObject> deletedLeaves = mapper.getNonMappedLeavesT1(); //deleted leaf statements
List<CompositeStatementObject> deletedComposites = mapper.getNonMappedInnerNodesT1(); //deleted composite statements
```
For the Extract Method Refactoring example shown above `mapper.getNonMappedLeavesT2()` returns the following statements:
```java
final String url = pageNumber == 0 ? "courses" : "courses?page=" + String.valueOf(pageNumber);
final CoursesContainer coursesContainer = getFromStepic(url,CoursesContainer.class);
return coursesContainer.meta.containsKey("has_next") && coursesContainer.meta.get("has_next") == Boolean.TRUE;
```
#2. You can use the following code snippet to obtain the **matched statements** between the original and the extracted methods:
```java
ExtractOperationRefactoring refactoring = ...;
UMLOperationBodyMapper mapper = refactoring.getBodyMapper();
for(AbstractCodeMapping mapping : mapper.getMappings()) {
  AbstractCodeFragment fragment1 = mapping.getFragment1();
  AbstractCodeFragment fragment2 = mapping.getFragment2();
  Set<Replacement> replacements = mapping.getReplacements();
  for(Replacement replacement : replacements) {
    String valueBefore = replacement.getBefore();
    String valueAfter = replacement.getAfter();
    ReplacementType type = replacement.getType();
  }
}
```
For the Extract Method Refactoring example shown above `mapping.getReplacements()` returns the following AST node replacement for the pair of matched statements:
```java
final List<CourseInfo> courseInfos = getFromStepic("courses",CoursesContainer.class).courses;
final List<CourseInfo> courseInfos = coursesContainer.courses;
```
**Replacement**: `getFromStepic("courses",CoursesContainer.class)` -> `coursesContainer`

**ReplacementType**: VARIABLE_REPLACED_WITH_METHOD_INVOCATION

#3. You can use the following code snippet to obtain the **overlapping refactorings** in the extracted method:
```java
ExtractOperationRefactoring refactoring = ...;
UMLOperationBodyMapper mapper = refactoring.getBodyMapper();
Set<Refactoring> overlappingRefactorings = mapper.getRefactorings();
```
For the Extract Method Refactoring example shown above `mapper.getRefactorings()` returns the following refactoring:

**Extract Variable** `coursesContainer : CoursesContainer` in method
`private addCoursesFromStepic(result List<CourseInfo>, pageNumber int) : boolean`
from class `com.jetbrains.edu.stepic.EduStepicConnector`

because variable `coursesContainer = getFromStepic(url,CoursesContainer.class)` has been extracted from the following statement of the original method by replacing string literal `"courses"` with variable `url`:
```java
final List<CourseInfo> courseInfos = getFromStepic("courses",CoursesContainer.class).courses;
```

# Running RefactoringMiner from the command line

When you build a distributable application with `./gradlew distZip`, you can run Refactoring Miner as a command line application. Extract the file under `build/distribution/RefactoringMiner.zip` in the desired location, and cd into the `bin` folder (or include it in your path). Then, run `RefactoringMiner -h` to show its usage:

    > RefactoringMiner -h

	-h											Show options
	-a <git-repo-folder> <branch> -json <path-to-json-file>					Detect all refactorings at <branch> for <git-repo-folder>. If <branch> is not specified, commits from all branches are analyzed.
	-bc <git-repo-folder> <start-commit-sha1> <end-commit-sha1> -json <path-to-json-file>	Detect refactorings between <start-commit-sha1> and <end-commit-sha1> for project <git-repo-folder>
	-bt <git-repo-folder> <start-tag> <end-tag> -json <path-to-json-file>			Detect refactorings between <start-tag> and <end-tag> for project <git-repo-folder>
	-c <git-repo-folder> <commit-sha1> -json <path-to-json-file>				Detect refactorings at specified commit <commit-sha1> for project <git-repo-folder>
	-gc <git-URL> <commit-sha1> <timeout> -json <path-to-json-file>				Detect refactorings at specified commit <commit-sha1> for project <git-URL> within the given <timeout> in seconds. All required information is obtained directly from GitHub using the OAuth token in github-oauth.properties
	-gp <git-URL> <pull-request> <timeout> -json <path-to-json-file>			Detect refactorings at specified pull request <pull-request> for project <git-URL> within the given <timeout> in seconds for each commit in the pull request. All required information is obtained directly from GitHub using the OAuth token in github-oauth.properties
	
With a locally cloned repository, run:

    > git clone https://github.com/danilofes/refactoring-toy-example.git refactoring-toy-example
    > ./RefactoringMiner -c refactoring-toy-example 36287f7c3b09eff78395267a3ac0d7da067863fd

If you don't want to clone locally the repository, run:

    > ./RefactoringMiner -gc https://github.com/danilofes/refactoring-toy-example.git 36287f7c3b09eff78395267a3ac0d7da067863fd 10

**For all options you can add the `-json <path-to-json-file>` command arguments to save the JSON output in a file. The results are appended to the file after each processed commit.**

For the `-gc` and `-gp` options you must provide a valid OAuth token in the `github-oauth.properties` file stored in the `bin` folder.
You can generate an OAuth token in GitHub `Settings` -> `Developer settings` -> `Personal access tokens`.

In both cases, you will get the output in JSON format:

    {
	"commits": [{
		"repository": "https://github.com/danilofes/refactoring-toy-example.git",
		"sha1": "36287f7c3b09eff78395267a3ac0d7da067863fd",
		"url": "https://github.com/danilofes/refactoring-toy-example/commit/36287f7c3b09eff78395267a3ac0d7da067863fd",
		"refactorings": [{
				"type": "Pull Up Attribute",
				"description": "Pull Up Attribute private age : int from class org.animals.Labrador to class org.animals.Dog",
				"leftSideLocations": [{
					"filePath": "src/org/animals/Labrador.java",
					"startLine": 5,
					"endLine": 5,
					"startColumn": 14,
					"endColumn": 21,
					"codeElementType": "FIELD_DECLARATION",
					"description": "original attribute declaration",
					"codeElement": "age : int"
				}],
				"rightSideLocations": [{
					"filePath": "src/org/animals/Dog.java",
					"startLine": 5,
					"endLine": 5,
					"startColumn": 14,
					"endColumn": 21,
					"codeElementType": "FIELD_DECLARATION",
					"description": "pulled up attribute declaration",
					"codeElement": "age : int"
				}]
			},
			{
				"type": "Pull Up Attribute",
				"description": "Pull Up Attribute private age : int from class org.animals.Poodle to class org.animals.Dog",
				"leftSideLocations": [{
					"filePath": "src/org/animals/Poodle.java",
					"startLine": 5,
					"endLine": 5,
					"startColumn": 14,
					"endColumn": 21,
					"codeElementType": "FIELD_DECLARATION",
					"description": "original attribute declaration",
					"codeElement": "age : int"
				}],
				"rightSideLocations": [{
					"filePath": "src/org/animals/Dog.java",
					"startLine": 5,
					"endLine": 5,
					"startColumn": 14,
					"endColumn": 21,
					"codeElementType": "FIELD_DECLARATION",
					"description": "pulled up attribute declaration",
					"codeElement": "age : int"
				}]
			},
			{
				"type": "Pull Up Method",
				"description": "Pull Up Method public getAge() : int from class org.animals.Labrador to public getAge() : int from class org.animals.Dog",
				"leftSideLocations": [{
					"filePath": "src/org/animals/Labrador.java",
					"startLine": 7,
					"endLine": 9,
					"startColumn": 2,
					"endColumn": 3,
					"codeElementType": "METHOD_DECLARATION",
					"description": "original method declaration",
					"codeElement": "public getAge() : int"
				}],
				"rightSideLocations": [{
					"filePath": "src/org/animals/Dog.java",
					"startLine": 7,
					"endLine": 9,
					"startColumn": 2,
					"endColumn": 3,
					"codeElementType": "METHOD_DECLARATION",
					"description": "pulled up method declaration",
					"codeElement": "public getAge() : int"
				}]
			},
			{
				"type": "Pull Up Method",
				"description": "Pull Up Method public getAge() : int from class org.animals.Poodle to public getAge() : int from class org.animals.Dog",
				"leftSideLocations": [{
					"filePath": "src/org/animals/Poodle.java",
					"startLine": 7,
					"endLine": 9,
					"startColumn": 2,
					"endColumn": 3,
					"codeElementType": "METHOD_DECLARATION",
					"description": "original method declaration",
					"codeElement": "public getAge() : int"
				}],
				"rightSideLocations": [{
					"filePath": "src/org/animals/Dog.java",
					"startLine": 7,
					"endLine": 9,
					"startColumn": 2,
					"endColumn": 3,
					"codeElementType": "METHOD_DECLARATION",
					"description": "pulled up method declaration",
					"codeElement": "public getAge() : int"
				}]
			}
		]
	}]
	}
