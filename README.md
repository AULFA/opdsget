opdsget
===

An OPDS feed retrieval and rewriting tool.

## Features

* Efficient parallel downloading of large feeds
* Transparent rewriting of feed URIs to make feeds readable offline
* Byte-for-byte reproducible feed archives (including stripping of time-related fields from feeds)
* Well designed modular API for use in Java 9 programs
* Command line interface

## Usage

```
Usage: opdsget [options]
  Options:
    --authentication
      The file containing authentication information
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
```

To download a feed `http://example.com/feed.atom` to directory
`/tmp/out`, assuming that `http://example.com/feed.atom` requires no
authentication, simply run the following:

```
$ java -jar org.aulfa.opdsget.cmdline-0.0.1-main.jar --feed http://example.com/feed.atom --output-directory /tmp/out
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

$ java -jar org.aulfa.opdsget.cmdline-0.0.1-main.jar @arguments.txt
```

The `opdsget` program is capable of producing a _reproducible_ zip
archive of any feed that it downloads. A _reproducible_ zip is a zip
archive with entries in alphabetical order, with any time-related
fields in the entry set of predictable values. The `opdsget` API
also removes frequently-changing time-related fields from feeds in
order to help ensure reproducible results. To produce a zip file
`/tmp/out.zip`, use the `--output-archive` option:

```
$ java -jar org.aulfa.opdsget.cmdline-0.0.1-main.jar --feed http://example.com/feed.atom --output-directory /tmp/out --output-archive /tmp/out.zip
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
then no authentication data is assumed.

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
```

The two currently supported authentication types are `none` (meaning no credentials
are sent with any request), and `basic` (HTTP Basic authentication).

Additionally, lines containing only whitespace, or starting with `#` are ignored.

Assuming the above example authentication file in `authentication.map`,
the feed can be fetched using authentication information with:

```
$ java -jar org.aulfa.opdsget.cmdline-0.0.1-main.jar --feed http://example.com/feed.atom --output-directory /tmp/out --output-archive /tmp/out.zip --authentication authentication.map
```

