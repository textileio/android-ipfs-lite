# android-ipfs-lite

[![Made by Textile](https://img.shields.io/badge/made%20by-Textile-informational.svg?style=flat-square)](https://textile.io)
[![Chat on Slack](https://img.shields.io/badge/slack-slack.textile.io-informational.svg?style=flat-square)](https://slack.textile.io)
[![GitHub license](https://img.shields.io/github/license/textileio/android-ipfs-lite.svg?style=flat-square)](./LICENSE)
[![Release](https://img.shields.io/github/release/textileio/android-ipfs-lite.svg?style=flat-square)](https://github.com/textileio/android-ipfs-lite/releases/latest)
[![CircleCI branch](https://img.shields.io/circleci/project/github/textileio/android-ipfs-lite/master.svg?style=flat-square)](https://circleci.com/gh/textileio/android-ipfs-lite)
[![docs](https://img.shields.io/badge/docs-master-success.svg?style=popout-square)](https://textileio.github.io/android-ipfs-lite/)
[![standard-readme compliant](https://img.shields.io/badge/standard--readme-OK-green.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)

> A lightweight, extensible IPFS peer for Android.

IPFS Lite runs the minimal setup required to get and put IPLD DAGs on the IPFS network. It is a port of the [Go IPFS Lite](https://github.com/hsanjuan/ipfs-lite) library.

## Table of Contents

- [android-ipfs-lite](#android-ipfs-lite)
  - [Table of Contents](#table-of-contents)
  - [Background](#background)
  - [Roadmap](#roadmap)
  - [Install](#install)
  - [Usage](#usage)
    - [Initialize and start a Peer](#initialize-and-start-a-peer)
    - [Add data](#add-data)
    - [Add a file](#add-a-file)
    - [Fetch a file by CID](#fetch-a-file-by-cid)
  - [Maintainers](#maintainers)
  - [Contributing](#contributing)
  - [License](#license)

## Background

IPFS Lite runs the minimal setup required to provide a DAG service. It is a port of the [Go IPFS Lite](https://github.com/hsanjuan/ipfs-lite) library, and as such, has the same requirements. The goal of IPFS Lite is to run the bare minimal functionality for any IPLD-based application to interact with the IPFS network (by getting and putting blocks). This saves having to deal with the complexities of using a full IPFS daemon, while maintaining the ability to share the underlying libp2p host and DHT with other components.

## Roadmap

- [x] Start IPFS Lite
- [x] Stop IPFS Lite
- [x] `GetFile(String cid)` Get file by Content Address.
- [x] `GetFileSync(String cid)` Synchronously get file by Content Address.
- [x] `AddFile(byte[] data)` Add file to IPFS.
- [x] `AddFileSync(byte[] data)` Synchronously Add file to IPFS.
- [x] `GetNode(String cid)` Get IPLD node.
- [x] `RemoveNode(String cid)` Remove IPLD node.
- [ ] Add IPLD node.
- [x] `ResolveLink(String link)`

## Install

The IPFS Lite library is published in [Textile's Bintray Maven repository](https://dl.bintray.com/textile/maven).
You can install it using Gradle.

First, you'll need to add Textile's Bintray Maven repository to you project's top level `build.gradle` in the `allProjects.repositories` section:

```cmd
allprojects {
    repositories {
        ...
        maven { url 'https://dl.bintray.com/textile/maven' }
        maven { url 'https://jitpack.io' }
        ...
    }
}
```

Next, add the IPFS Lite dependency to your app module's `build.gradle` `dependencies` section, specifying the [latest version available](https://bintray.com/textile/maven/ipfs-lite/_latestVersion):

```cmd
dependencies {
    ...
    implementation 'io.textile:ipfs-lite:0.0.1-rc4'
    ...
}
```

## Usage

### Initialize and start a Peer

```java
  Boolean debug = false;
  Peer litePeer = new Peer('/path/', debug);
  litePeer.Start();
```

* To learn see how a path is choosen, see the [test suite example](https://github.com/textileio/android-ipfs-lite/blob/master/ipfslite/src/androidTest/java/io/textile/ipfslite/PeerTest.java#L38).

### Add data

```java
  String message = "Hello World";
  String cid = litePeer.AddFileSync(message.getBytes());
```

### Add a file

```java
  File file = openFile("secret_plans");
  byte[] bytes = Files.readAllBytes(file.toPath());
  String cid = litePeer.AddFileSync(bytes);

  // OR Asynchronously
  litePeer.AddFile(bytes, resultHandler);
```

### Fetch a file by CID

```java
  byte[] data = litePeer.GetFileSync("bafybeic35nent64fowmiohupnwnkfm2uxh6vpnyjlt3selcodjipfrokgi");
  // OR Asynchronously
  litePeer.GetFile("bafybeic35nent64fowmiohupnwnkfm2uxh6vpnyjlt3selcodjipfrokgi", resultHandler);
```

## Maintainers

[Andrew Hill](https://github.com/andrewxhill)

## Contributing

See [the contributing file](CONTRIBUTING.md)!

PRs accepted.

Small note: If editing the README, please conform to the [standard-readme](https://github.com/RichardLitt/standard-readme) specification.

## License

[MIT](LICENSE) (c) 2019 Textile
