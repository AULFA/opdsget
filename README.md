opdsget
===

An OPDS feed retrieval and rewriting tool.

## Features

* Efficient parallel downloading of large feeds
* Transparent rewriting of feed URIs to make feeds readable offline
* Byte-for-byte reproducible feed archives (including fixing of time-related fields from feeds)
* Well designed modular API for use in Java 9 programs
* Command line interface

## Requirements

* Java 11+

## How To Build

```
$ mvn clean package
```

If the above fails, it's a bug. Report it!

## Usage

```
Usage: opdsget [options]
  Options:
    --authentication
      The file containing authentication information
    --exclude-content-kind
      The kind of content that will not be downloaded (Specify multiple times 
      for multiple kinds)
      Default: []
  * --feed
      The URI of the remote feed
    --log-level
      The logging level
      Default: info
      Possible Values: [error, info, debug, trace]
    --output-archive
      The zip archive that will be created for the feed
  * --output-directory
      The directory that will contain the downloaded feed objects
    --uri-rewrite-scheme
      The scheme that will be used for rewritten URIs
      Default: file
```

To download a feed `http://example.com/feed.atom` to directory
`/tmp/out`, assuming that `http://example.com/feed.atom` requires no
authentication, simply run the following:

```
$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.1-main.jar \
  --feed http://example.com/feed.atom \
  --output-directory /tmp/out
```

The `opdsget` package uses [jcommander](http://jcommander.org) to
parse command line arguments and therefore also supports placing
command line options into a file that can be referenced with `@`:

```
$ cat arguments.txt
--feed
http://example.com/feed.atom
--output-directory
/tmp/out

$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.1-main.jar @arguments.txt
```

## Archiving/Rewriting

The `opdsget` program is capable of producing a _reproducible_ zip
archive of any feed that it downloads. A _reproducible_ zip is a zip
archive with entries in alphabetical order, with any time-related
fields in the entry set to fixed values. The `opdsget` API
also sets frequently-changing time-related fields from feeds to
fixed values in order to help ensure reproducible results. To produce
a zip file `/tmp/out.zip`, use the `--output-archive` option:

```
$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.1-main.jar \
  --feed http://example.com/feed.atom \
  --output-directory /tmp/out \
  --output-archive /tmp/out.zip
```

Each downloaded object is stored in the output directory (and therefore,
by extension, the resulting zip file) both by the `SHA256` hash of the
original URI and the type of the file. Links inside feeds are rewritten
so that they point to files within the output directory (using relative
paths). For example, a feed at `http://www.example.com/feed.atom` will
be placed in the output directory `out` at `out/feeds/DD1E9BA1ECF8D7B30994CB07D62320DE5F8912D8DF336B874489FD2D9985AEB2.atom`.
Any reference to `http://www.example.com/feed.atom` in subsequent feeds
will be rewritten to `file://feeds/DD1E9BA1ECF8D7B30994CB07D62320DE5F8912D8DF336B874489FD2D9985AEB2.atom`
by default. It's possible to specify a custom URI scheme that will
be used for rewritten links. This is useful for applications that
wish to embed OPDS feeds and need to use a custom URI scheme to refer
to bundled content in a manner distinct from non-bundled remote content.
The `--uri-rewrite-scheme` is used to specify the scheme:

```
$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.1-main.jar \
  --feed http://example.com/feed.atom \
  --output-directory /tmp/out \
  --uri-rewrite-scheme bundled-example
```

This will result in feeds containing links such as `bundled-example://feeds/DD1E9BA1ECF8D7B30994CB07D62320DE5F8912D8DF336B874489FD2D9985AEB2.atom`.

In some cases, it may be desirable to fetch the feeds but not the
content (such as book images and/or the actual book files themselves).
The `--exclude-content-kind` parameter may be specified one or more
times to exclude specific content. For example, the following invocations
will cause `opdsget` to avoid downloading book images, avoid downloading
books, and avoid downloading anything other than feeds, respectively:

```
$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.1-main.jar \
  --feed http://example.com/feed.atom \
  --exclude-content-kind images \
  --output-directory /tmp/out

$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.1-main.jar \
  --feed http://example.com/feed.atom \
  --exclude-content-kind books \
  --output-directory /tmp/out

$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.1-main.jar \
  --feed http://example.com/feed.atom \
  --exclude-content-kind images \
  --exclude-content-kind books \
  --output-directory /tmp/out
```

Content that is excluded by the `--exclude-content-kind` parameter
will _not_ be affected by the `--uri-rewrite-scheme` option; only
content that is actually downloaded will have rewritten links. By
default, `opdsget` excludes nothing.

## Authentication

The `opdsget` command line program supports a flexible pattern-based
method to supply authentication data when downloading feeds. Many
real-life OPDS feeds are spread across multiple domains and a single
feed can require different types of authentication (or sometimes no
authentication at all for specific links).

The `opdsget` command uses a simple line-based file format to specify
patterns against which URIs are matched. Patterns are matched against
URIs in the order that they are given in the file, stopping at the
first pattern that matches. If no pattern matches the incoming URI,
then no authentication data is assumed. Patterns use
[Java regular expression syntax](https://docs.oracle.com/javase/9/docs/api/java/util/regex/Pattern.html)
and are matched against the entire URI including the scheme (`http://`, `https://`, etc).

An example authentication file:

```
# URIs ending with "download" refer to books and specifically require no authentication
http[s]?://www\.example\.com/media/.*/download  none

# Otherwise, feeds and images require auth, but no other domains do
http[s]?://www\.example\.com(/.*)?  basic:rblake:SizingFrightfulStiltRemovedMarsupialJukebox
```

The syntax of the file is given by the following [EBNF](https://en.wikipedia.org/wiki/Extended_Backus%E2%80%93Naur_form):

```
pattern = ? any java.util.Pattern ?

line_terminator = ( U+000D U+000A | U+000A ) ;

line = pattern , authentication , line_terminator ;

authentication =
    'none'
  | 'basic' , ':' , user , ':' , password ;

file = { line } ;
```

The two currently supported authentication types are `none` (meaning no credentials
are sent with any request), and `basic` (HTTP Basic authentication).

Additionally, lines containing only whitespace, or starting with `#` are ignored.

Assuming the above example authentication file in `authentication.map`,
the feed can be fetched using authentication information with:

```
$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.1-main.jar \
  --feed http://example.com/feed.atom \
  --output-directory /tmp/out \
  --output-archive /tmp/out.zip \
  --authentication authentication.map
```

