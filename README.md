# Java Object Layout (JOL) plugin for IntelliJ Idea

[JOL](http://openjdk.java.net/projects/code-tools/jol/) (Java Object Layout) is the tiny toolbox to analyze object layout schemes in JVMs.
For example, in HotSpot VM on 64x processor an empty string takes 40 bytes i.e. 24 bytes for String object itself + 16 bytes for an internal empty char array.

The plugin is a GUI for JOL and allows you to make an estimate how much memory the object takes. Thus you can perform a simplest but most efficient performance improvements. Just check your DTOs if they fit into 64 bytes of processor's cache line.
  
ATM the plugin supports only basic estimate of class layout in different VM modes i.e. the same as `jol-cli estimates` command.

**NOTE:** Your app most likely will use the `64-bit VM, compressed references` mode. 

## Install the plugin
Open File / Settings / Plugins  then type `JOL` in search input and press `Browse in repositories` button.

## Usage
Set a cursor into a class name and then press Code / Show Object Layout and you'll see a right panel with layout info.

![screenshot.png](screenshot.png)

