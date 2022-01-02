val repo = "eie"
name := repo

libraryDependencies ++= List(
  "org.scalatest"        %% "scalatest"               % "3.2.10"  % Test,
  "org.pegdown"          % "pegdown"                  % "1.6.0"  % Test,
  "junit"                % "junit"                    % "4.13.2"   % Test,
  "com.vladsch.flexmark" % "flexmark-profile-pegdown" % "0.62.2" % Test,
  "javax.xml.bind"       % "jaxb-api"                 % "2.3.1"  % "provided"
)

val username     = "aaronp"

organization := s"com.github.${username}"
ThisBuild / scalaVersion := "3.1.0"
ThisBuild / resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
enablePlugins(GitVersioning)
enablePlugins(GhpagesPlugin)
enablePlugins(PamfletPlugin)
enablePlugins(SiteScaladocPlugin)

sourceDirectory in Pamflet := sourceDirectory.value / "site"

autoAPIMappings := true
exportJars := false
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-XX:MaxMetaspaceSize=1g")
git.useGitDescribe := false

// coverallsTokenFile := Option((Path.userHome / ".sbt" / ".coveralls.eie").asPath.toString)

//scalacOptions ++= List(scalaVersion.value)
//  .filter(_.contains("2.13"))
//  .map(_ => "-Ymacro-annotations")

ThisBuild / scalacOptions ++= List(
  "-language:implicitConversions",
//  "-source:3.0-migration",
//  "-rewrite",
//  "-new-syntax",
//  "-indent",
)

//addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
buildInfoPackage := s"${repo}.build"

// see http://www.scalatest.org/user_guide/using_scalatest_with_sbt
Test / testOptions += (Tests.Argument(TestFrameworks.ScalaTest, "-h", s"target/scalatest-reports", "-oN"))

// see https://www.scala-sbt.org/sbt-site/api-documentation.html
SiteScaladoc / siteSubdirName := "api/latest"

scalacOptions in (Compile, doc) ++= Seq("-groups", "-implicits")

packageOptions in (Compile, packageBin) += Package.ManifestAttributes("git-sha" -> git.gitHeadCommit.value.getOrElse("unknown"))

git.gitTagToVersionNumber := { tag: String =>
  if (tag matches "v?[0-9]+\\..*") {
    Some(tag)
  } else None
}

coverageMinimum := 80
coverageFailOnMinimum := true

// see http://scalameta.org/scalafmt/
ThisBuild / scalafmtOnCompile := true
ThisBuild / scalafmtVersion := "1.4.0"

// Define a `Configuration` for each project, as per http://www.scala-sbt.org/sbt-site/api-documentation.html
//val Api = config("api")

// see https://github.com/sbt/sbt-ghpages
// this exposes the 'ghpagesPushSite' task

git.remoteRepo := s"git@github.com:$username/$repo.git"
ghpagesNoJekyll := true

lazy val settings = scalafmtSettings

releasePublishArtifactsAction := PgpKeys.publishSigned.value

test in assembly := {}

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

// see https://leonard.io/blog/2017/01/an-in-depth-guide-to-deploying-to-maven-central/
pomIncludeRepository := (_ => false)

// To sync with Maven central, you need to supply the following information:
Global / pomExtra := {
  <url>https://github.com/${username}/${repo}
  </url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>
          ${username}
        </id>
        <name>Aaron Pritzlaff</name>
        <url>https://github.com/${username}/${repo}
        </url>
      </developer>
    </developers>
}
