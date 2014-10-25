import net.rosien.landslide._

scalaVersion := "2.10.3"

libraryDependencies ++=
  "org.typelevel" %% "scalaz-contrib-210"        % "0.1.5" ::
  ("com.chuusai" %% "shapeless"   % "2.0.0" cross CrossVersion.full) ::
  Nil

tutSettings

site.settings

LandslideSupport.settings ++ Seq(site.addMappingsToSiteDir(mappings in LandslideSupport.Landslide, ""))

LandslideSupport.Destination := "index.html"

ghpages.settings

git.remoteRepo := "git@github.com:arosien/lovely-for-comps.git"
