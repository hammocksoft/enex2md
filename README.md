# enex2md - a Evernote to Markdown converter

Evernote is a great tool for managing your notes, but it has one mayor disadvantage:
 it stores all your data in an unencrypted form so you completly loose control.
 
When looking for an alternative solution, I came accross this great tool called [http://elephant.mine.nu/]["Elephant"]. 

It's obviously inspired by Evernote and stores all notes in a clean directory structure 
as single files in Markdown (with embedded files in a subdirectory). 
Markdown itself is a very simple and clean text format, perfect for notes or shorter documents.

Furthermore, it's open source and written in Java - a language that I've been using 
for almost 20 years, so I feel confident that I can fix any issues in case the program stops 
working for whatever reason.

But how to get from Evernote to Markdown?

In my case, we're talking 425 notes with lost of embedded images and pdfs so copy-pasting 
the notes into Elephant is not an option.

I found 2 tools that claim to to the conversion, but they both seemed quite immature, 
simply exporting the HTML which Evernote uses internally as is. Not really helpful.
 
So, beeing a coder I started what coders do: write my own tool. 
 
## Features
 
* underline, bold, italics
* text with monospace font exported as code blocks
* merge consecutive code blocks into one
* webclips are stored as .html
* lists (bullets and numbered) and nested lists
* tables + colspans
* embedded / attached files (png, jpg, pdf)

## TODO

* strikethrough
* convert text sizes to different title sizes based on default size
* support for multiple notes with the same name (add counter to the file name)

## Not supported

* Font sizes

## How to use

1. Export your notes from Evernote

Select all the notes in one notebook by selecting them and the clicking File -> Export Note. 

Use the name of the notebook as the file name. All the notes will be saved in a single file.

If you export all of your notes (across all notebooks) at ones, you'll end up with all 
of them in a single directory.
  
2. Convert  
 
Open a Terminal window, change to the directory where your exported file is located and run 

java -jar /path/to/enex2md.jar file.enex

You will get a directory with the same name as your exported file (minus the .enex) containing 
all your notes in Markdown


3. Copy your exported files/folders into the Elephant directory

## Store your notes in the Cloud - encrypted.

4.1 Use Sync.com (End-to-end encrypted)

4.2 Use encfs + Dropbox
Referral Link: You get 500 MB of extra space, I get 1 GB.
https://db.tt/Md1QDQes

