opdsget
===

An OPDS feed retrieval and rewriting tool.

![opdsget](./src/site/resources/opdsget.jpg?raw=true)

## Features

* Efficient parallel downloading of large feeds
* Transparent rewriting of feed URIs to make feeds readable offline
* Byte-for-byte reproducible feed archives (including fixing of time-related fields from feeds)
* Search index generation for feeds
* Well designed modular API for use in Java 11+ programs
* Command line interface
* Cover image scaling and recompression
* EPUB squashing with [epubsquash](https://github.com/AULFA/epubsquash)

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
    --scale-cover-images
      A scale value in the range (0.0, 1.0] by which to scale cover images
      Default: 1.0
    --squash
      True if EPUB files should be squashed to reduce their size
      Default: false
    --squash-image-max-height
      The maximum height of images
      Default: 1170.0
    --squash-image-max-width
      The maximum width of images
      Default: 1600.0
    --uri-rewrite-scheme-name
      The name of the URI scheme used to rewrite URIs (if applicable)
      Default: file
    --uri-rewrite-strategy
      The strategy that will be used to rewrite URIs
      Default: RELATIVE
      Possible Values: [NONE, NAMED, RELATIVE]

```

To download a feed `http://example.com/feed.atom` to directory
`/tmp/out`, assuming that `http://example.com/feed.atom` requires no
authentication, simply run the following:

```
$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.5-main.jar \
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

$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.5-main.jar @arguments.txt
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
$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.5-main.jar \
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
by default.

It's possible to specify the strategy used to rewrite links with
`--uri-rewrite-strategy`. The following strategies are available
in the command-line interface:

  * `NONE`: Links are not rewritten at all
  * `RELATIVE`: Links are rewritten to be relative to the generated
     files.
  * `NAMED`: Links are rewritten to use a named URI scheme and
     prefixes are removed such that the files appear to be in the
     "root directory" of a filesystem.

For the `NAMED` strategy, `--uri-rewrite-scheme-name` is used to
specify the scheme:

```
$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.5-main.jar \
  --feed http://example.com/feed.atom \
  --output-directory /tmp/out \
  --uri-rewrite-strategy NAMED \
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
$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.5-main.jar \
  --feed http://example.com/feed.atom \
  --exclude-content-kind images \
  --output-directory /tmp/out

$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.5-main.jar \
  --feed http://example.com/feed.atom \
  --exclude-content-kind books \
  --output-directory /tmp/out

$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.5-main.jar \
  --feed http://example.com/feed.atom \
  --exclude-content-kind images \
  --exclude-content-kind books \
  --output-directory /tmp/out
```

Content that is excluded by the `--exclude-content-kind` parameter
will _not_ be affected by the `--uri-rewrite-scheme` option; only
content that is actually downloaded will have rewritten links. By
default, `opdsget` excludes nothing.

## Search Index

The `opdsget` tool constructs a rudimentary search index for feeds.

When the `opdsget` tool encounters a document with an Atom `entry`
element as the root element, it locates the `title` element and breaks
it up into whitespace-delimited words. It then associates each of those
words with the URI of the document. The associations between these
collected words and document URIs are recorded into a file,
`index.txt`, in a simple line-based format:

```
term = ? any uppercase alphanumeric string ?

uri = ? any valid URI ?

line_terminator = ( U+000D U+000A | U+000A ) ;

line = term , uri , line_terminator ;

file = { line } ;
```

An example index:

```
ADVENTURE simplified-bundled://feeds/D8038E93C488F0385F94D4F42E0FF481D9946C609254E600CF5EEEB7E51C14B1.atom
ADVENTURE simplified-bundled://feeds/DAD0E0D07CE3FCE49D620EABE32669805D90D3C8320E95A3F6AC30A09CD955DE.atom
ADVENTURES simplified-bundled://feeds/28AB7827DE7A98C2302A9DDC4046AC49FC4897DFFCF227A90838C30DB3E6257C.atom
AIVA simplified-bundled://feeds/F5713780AA60C83FA6C8523BA5568BE42BFBE8F5209E730A6BFADD4AE6823C3D.atom
ALICE simplified-bundled://feeds/84ACFE1FA1DC4D87DE0FB571F85027435553388272711B9BF7EA0046D0EFCEEF.atom
ALICES simplified-bundled://feeds/28AB7827DE7A98C2302A9DDC4046AC49FC4897DFFCF227A90838C30DB3E6257C.atom
ALPHABET simplified-bundled://feeds/7CFE34EF40DA0A4B4249DC652998C70700F527782E3C3CFC68683C16D9A056B2.atom
```

## Image Scaling

The `opdsget` command line program provides an option, `--scale-cover-images`,
that can optionally rescale and compress images that occur in OPDS
feeds. It takes a real value in the range `(0.0, 1.0])` and scales
cover images by that value.

For example, to scale all cover images by 50%:

```
$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.5-main.jar \
  --feed http://example.com/feed.atom \
  --output-directory /tmp/out \
  --output-archive /tmp/out.zip \
  --scale-cover-images 0.5
```

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
$ java -jar au.org.libraryforall.opdsget.cmdline-0.0.5-main.jar \
  --feed http://example.com/feed.atom \
  --output-directory /tmp/out \
  --output-archive /tmp/out.zip \
  --authentication authentication.map
```

## Squashing

EPUB files can optionally be _squashed_ with
[epubsquash](https://github.com/AULFA/epubsquash).  Squashing
essentially unpacks an EPUB file, compresses overly-large images,
and then repacks the EPUB resulting in a hopefully-much-smaller
file. Use the `--squash` parameter to enable squashing, and the
`--squash-image-max-width` and `--squash-image-max-height` parameters
to control how images should be resized.

