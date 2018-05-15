package better.files
package test_pkg

import java.io.InputStream

class ResourceSpecHelper {
  def openTestStream(): InputStream = Resource.my.asStream("another-test-file.txt").get
}
