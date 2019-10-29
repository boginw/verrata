# Verrata

Compare texts without opening files.

## Reasoning

There are times when GitHub just gives up on a line and pretends that the whole line has been changed (see the picture 
below for reference). This is very annoying when performing pull request reviews, as you really have no idea what has 
been changed without reading it thoroughly.

![Github](screenshots/github.png?raw=true)

The idea is to have Verrata doing the comparing when GitHub fails. Whenever I see GitHub pretending that a whole line
has changed, I can copy the old and new value into Verrata and it will tell me precisely what has changed, as you can 
see on the picture bellow.

![Github](screenshots/verrata.png?raw=true)

I'm not going to pretend that this problem is not already solved, it's just that I don't like the existing solutions. 
Websites like [diffchecker.com](https://www.diffchecker.com/) do precisely what this application is meant to solve, but
personally, I like tools that I use all the time to be available offline, and to have quick access to these tools
through my operating system instead of my browser. 

## Libs

* JavaFX
* RichtextFX
* java-diff-utils