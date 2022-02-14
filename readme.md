# DeeplinkBuilder [![License: Apache 2.0,][license-img]][license-url] [![at Gradle Plugin Portal,][gradle-img]](gradle-url) [![Stargazers at Github][star-img]][star-url]

DeeplinkBuilder is a gradle plugin to fix a weakness of the navigation components of Google. When
you have a navigation graph with sub graphs (e.g. because you use BottomNavigationBar) then you
cannot link directly from one part of the graph to another. This will cause a runtime crash and is
as discussed in some Bugreports intended behavior. You should use deeplinks for this, however
deeplinks are not type safe and the urls can easily out date and cause bugs and even more crashes.
Here comes this plugin in place: A small class is generated which create a `NavDeepLinkRequest` for
you with the support to insert the variable parts.

# Sample

You can find a full sample in the [sample-app](./sample-app) directory. Here in brief the
interesting parts:

## The navigation graph
```xml
<fragment
    android:id="@+id/frag_book"
    android:name="my.package.BookFragment"
    android:label="Book Fragment"
    tools:layout="@layout/fragment_book">
    <argument
        android:name="name"
        app:argType="string"/>
    <deepLink app:uri="my.package://shelf/{name}/" />
</fragment>
```
<sup>[Jump to source](./sample-app/src/main/res/navigation/sample.xml#L53-L62)</sup>

## Usage

1. Using the [plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):

    ```groovy
    plugins {
      id "eu.rekisoft.android.deeplink-builder" version "1.0.0" // add this line
    }
    ```

2. Add the plugin to your gradle module:

    ```groovy
    plugins {
      id 'com.android.application'
      id 'kotlin-android'
      id 'androidx.navigation.safeargs.kotlin'
      id 'eu.rekisoft.android.deeplink-builder' version '1.0.0' // add this line
    }
    ```

3. Using the deeplinks in your code:

    ```kotlin
    findNavController().navigate(BookFragmentDeeplink.create("example"))
    ```
    <sup>[Jump to source](./sample-app/src/main/java/eu/rekisoft/android/deeplink/HomeFragment.kt#L22)</sup>  
    When you have multiple deeplinks then its id will be used as method name. Each method has just the
    minimal set of arguments required for building the deeplink with the default parameters as defined
    in the xml.

# Development

1. You need to checkout this repository and if the version is newer as on the plugin portal temporally
   disable the plugin usage by commenting out the line in `sample-app/build.gradle`:

    ```groovy
    id 'eu.rekisoft.android.deeplink-builder' version '1.0.0'
    ```

2. Deploy the plugin to your local maven repository:

    ```shell
    ./gradlew publishToMavenLocal
    ```
    
3. Undo step 1
4. Test your changes
5. Create a Pull Request

# License

Apache License 2.0

[license-img]: https://img.shields.io/github/license/rekire/DeeplinkBuilder
[license-url]: ./LICENSE
[gradle-img]: https://img.shields.io/gradle-plugin-portal/v/eu.rekisoft.android.deeplink-builder
[gradle-url]: https://plugins.gradle.org/plugin/eu.rekisoft.android.deeplink-builder
[star-img]: https://img.shields.io/github/stars/rekire/DeeplinkBuilder.svg?style=social&label=Star&maxAge=3600
[star-url]: https://github.com/rekire/DeeplinkBuilder/stargazers
