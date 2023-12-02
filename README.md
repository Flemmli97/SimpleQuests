# Simple Quests
[![](http://cf.way2muchnoise.eu/full_580853_Fabric_%20.svg)![](http://cf.way2muchnoise.eu/versions/580853.svg)](https://www.curseforge.com/minecraft/mc-mods/simple-quests) 
[![](http://cf.way2muchnoise.eu/full_580854_Forge_%20.svg)![](http://cf.way2muchnoise.eu/versions/580854.svg)](https://www.curseforge.com/minecraft/mc-mods/simple-quests-forge)  
[![](https://img.shields.io/modrinth/dt/HriwQx5q?logo=modrinth&label=Modrinth)![](https://img.shields.io/modrinth/game-versions/HriwQx5q?logo=modrinth&label=Latest%20for)](https://modrinth.com/mod/simple-quests)  
[![Discord](https://img.shields.io/discord/790631506313478155?color=0a48c4&label=Discord)](https://discord.gg/8Cx26tfWNs)

Server side questing mod

If you would like to contribute a language PR into this mod with your lang file.  
The lang files go under `common/src/resources/data/simplequests/lang`


To use this mod as a dependency add the following snippet to your build.gradle:  
```groovy
repositories {
    maven {
        name = "Flemmli97"
        url "https://gitlab.com/api/v4/projects/21830712/packages/maven"
    }
}

dependencies {    
    //Fabric/Loom==========    
    modImplementation("io.github.flemmli97:simplequests:${minecraft_version}-${mod_version}-${mod_loader}")
    
    //Forge==========    
    compile fg.deobf("io.github.flemmli97:simplequests:${minecraft_version}-${mod_version}-${mod_loader}")
}
```