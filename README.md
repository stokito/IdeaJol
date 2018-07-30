# Java Object Layout (JOL) plugin for IntelliJ Idea

[jol](http://openjdk.java.net/projects/code-tools/jol/) is the tiny toolbox to analyze object layout schemes in JVMs.
These tools are using Unsafe, JVMTI, and Serviceability Agent (SA) heavily to decoder the actual object layout, footprint, and references.
This makes JOL much more accurate than other tools relying on heap dumps, specification assumptions, etc.

This plugin supports only basic estimate of class layout in different VM modes i.e. the same as `jol-cli estimates` command.

**NOTE:** Your app most likely will use the `64-bit VM, compressed references` mode. 

## Install the plugin
Open File / Settings / Plugins  then press `Install from Disk` button and select the [IdeaJol.zip](https://github.com/stokito/IdeaJol/releases/download/v1.3.0/IdeaJol.zip) file.

## Usage
Set a cursor into a class name and then press Code / Show Object Layout and you'll see a right panel with layout info.

![screenshot.png](screenshot.png)

