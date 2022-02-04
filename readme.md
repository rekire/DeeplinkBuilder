# DeeplinkBuilder

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
```kotlin
findNavController().navigate(BookFragmentDeeplink.create("example"))
```
<sup>[Jump to source](./sample-app/src/main/java/eu/rekisoft/android/deeplink/HomeFragment.kt#L22)</sup>

# Setup

Currently this plugin is not yet published. Therefore you need to build it yourself for now:

1. You just need to checkout this repository and execute then:

    ```shell
    ./gradlew publishToMavenLocal
    ```

2. Add your local maven repository to the gradle plugin repositories in your `settings.gradle`:
    ```groovy
    pluginManagement {
      repositories {
        mavenLocal() // just for plugin development
        mavenCentral()
        gradlePluginPortal()
      }
    }
    ```

3. Add the plugin to your project:

    ```groovy
    plugins {
      id 'com.android.application'
      id 'kotlin-android'
      id 'androidx.navigation.safeargs.kotlin'
      id 'eu.rekisoft.android.deeplink-builder' version '0.1' // add this line
    }
    ```

4. Profit!

# License

Apache License 2.0
