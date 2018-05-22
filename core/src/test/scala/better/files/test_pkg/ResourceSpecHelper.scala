package better.files
package test_pkg

import java.io.InputStream

class ResourceSpecHelper {
  def openTestStream(): InputStream = Resource.my.getAsStream("another-test-file.txt")
}
