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

## Code Reviewer Perspective
From a code reviewer point of view, having the knowledge about purity of refactorings gives the reviewer a deeper insight into the code modifications. For instance, the reviewer can simply skip the changes happening within pure refactorings, as she can be sure that there is no functionality change involved within those cases.

## Developer Perspective
Many developers prefer manual refactoring over automated tools. On the other hand, recent research has indicated that manual refactorings are error-prone. PurityCehcker with the ability to detect whether a refactoring is pure or not can help the developer to prevent erroneous application of manual refactoring. During our manual validation, we encountered some real-world cases in which the impureness of a refactoring can be considered as a hint to inform the developer that the refactoring is being done incorrectly.
<br><br>

In this example, the developer extracts many instances of duplicated code concerning different sorting algorithms into the `runTest()`. The refactoring essentially parameterizes the sorting algorithm with the Testable parameter. However, because the refactoring is done manually, the developer makes a mistake. She does not replace the algorithm `InsertionSort` with parameter `testable` in all places in the extracted method. 


<img width="676" alt="image" src="https://github.com/pedramnoori/RefactoringMiner/assets/37044356/fd82eaa6-35c4-4936-b8d1-7fd33e300b6b">

<br>
<br>

The incorrect manual refactoring is fixed in a later commit with the commit message “Fixed a small mis-type in sort timing code” by replacing `InsertionSort` with parameter `testable`.

<br>

<img width="665" alt="image" src="https://github.com/pedramnoori/RefactoringMiner/assets/37044356/c3d3ba40-cd05-4efc-a22c-ec340f08090d">

<br>

PurityChecker would be able to warn the developer in these cases about non-behavior-preserving manual refactoring before committing the refactoring to the repository.

## Researher Perspective
PurityChecker can be used to conduct large-scale empirical studies in the Software Refactoring field. Almost every empirical study in the field of Software Refactoring can take advantage of PurityChecker. For instance, one might study the relation of pure and impure refactorings with software metrics (https://github.com/pedramnoori/RefactoringMiner/issues/3). 

## Regression Testing

Numerous studies propose diverse approaches for **Regression Test Selection** (**RTS**). Having a tool capable of determining whether a refactoring is pure or not can significantly benefit this field. Predicting a refactoring as pure enables the skipping of tests related to the changes influenced by these refactorings. This practice can profoundly impact regression testing by substantially reducing the number of tests that need to be executed. **Since pure refactorings preserve the program’s behavior, changes related to them do not require retesting.** 

<br>
In a study by <a href="https://ieeexplore.ieee.org/document/8453082" target="_blank">Wang et al</a>, the authors introduced refactoring-aware regression test selection. They argue that changes associated with refactorings are behavior-preserving, and tests covering these changes can be skipped. While this holds true for refactorings deemed behavior-preserving, it may not always be the case. Impure refactorings modify the program’s behavior, necessitating testing for changes within this category during regression testing. PurityChecker can greatly assist this work by focusing their study on pure refactoring operations, aiding in the selection of tests to be excluded from regression testing.



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
