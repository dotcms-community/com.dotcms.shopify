# dotcli for the win!

When developing this plugin, it can be necessary to make iterative edits to the files and upload them into dotCMS.  That can become painful and slow doing this manually. Using the dotcli, the whole process can be automated so that you can edit the .vtls and graphql files inline and refresh your browser and see the changes.  Heres how:

## Using dotcli for .vtl development


### Install dotcli
```
npm install -g @dotcms/dotcli
```
### Config and Authenticate with dotcli
Run 
```
dotcli config
```
and enter the information for your dotcms environment - name and url.

Then run 
```
dotcli login
``` 
to authenticate

You are now connected with your dotcms instance.  

## Use the development Script
Once you have configured and inited your dotcli (from the root of this repo) you can run the `./dotcli-sync.sh` script.  It will do the following:

1. Pull the files from dotCMS into a local folder
2. Watch the folder for changes and push them to dotCMS

This means that you can run `code .` and start editing the .vtl and .gql files in the `src/main/java/com.dotcms.shopify/resources` folder and start editing and they will be pushed to dotCMS.  You can then refresh your browser and see the changes.  This is a great way to develop the plugin.`






## General use of dotcli
### Pull the assets you want to edit
Let's say I want to edit a theme on the starter site.  I can run the cli to pull the assets from only that folder
```
dotcli files pull //demo.dotcms.com/application/themes/travel
```

This will drop those files into a folder tree that identifies where those folders live in dotCMS, relative to where you ran the command.  It would look something like:
```
./files/live/en-us/demo.dotcms.com/themes/travel
```

There are a lot of switches on the dotcli pull.  For any of the dotcli command you can run:
```
dotcli files pull --help
```

You can open up the folder and start editing to assets - `code .`

### Push the assets back
And here is the magic - coding in VSCode while pushing with --watch enabled.  This will monitor the downloaded files and will sync any changes that are made to them with dotCMS.  It can handle large directories and if

```
dotcli files push -w 2 ./files/live/en-us/demo.dotcms.com
```
