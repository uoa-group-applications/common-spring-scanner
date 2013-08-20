package nz.ac.auckland.common.scanner.spring

import groovy.transform.CompileStatic
import nz.ac.auckland.common.scanner.MultiModuleConfigScanner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

/**
 * This class appears to get used at least twice - Spring scanning for varioous resources. We can be efficient about it.
 * author: Richard Vowles - http://gplus.to/RichardVowles
 */
@CompileStatic
abstract class SpringScanner {
  private static final Logger log = LoggerFactory.getLogger(SpringScanner)
  /**
   * Tells us what we are looking for
   * @return pattern to match, e.g. classpath*:/i18n/messages*.properties
   */
  protected abstract String getResourceMatchingPattern()

  protected boolean includeJars = true
  protected boolean includeWarUnderlays = true
  protected boolean includeResources = true // .../resources/ directories
  protected boolean includeWebapps = true  // .../webapp/ directories

  protected ClassLoader springScannerClassLoader
  protected boolean inDevMode;

  protected ClassLoader scanClassLoader() {
    inDevMode = MultiModuleConfigScanner.inDevMode()

    if (inDevMode) {
      def urls = []

      MultiModuleConfigScanner.scan(new MultiModuleConfigScanner.Notifier(){
        @Override
        void underlayWar(URL url) throws Exception {
          if (includeWarUnderlays)
            urls.add(url)
        }

        @Override
        void jar(URL url) throws Exception {
          if (includeJars)
            urls.add(url)
        }

        @Override
        void dir(URL url) throws Exception {
          if (url.toString().endsWith("/webapp/") && includeWebapps)
            urls.add(url)

          if (url.toString().endsWith("/resources/") && includeResources)
            urls.add(url)
        }
      })

      log.debug("${this.class.simpleName} scanning: ${urls.collect({it.toString()}).join(',')}")

      URL[] warUrls = new URL[urls.size()]
      urls.toArray(warUrls)

      springScannerClassLoader = new URLClassLoader(warUrls, (ClassLoader)null)
    } else {
      springScannerClassLoader = this.getClass().getClassLoader() // default to running not in dev mode (war is on classpath)
    }

    return springScannerClassLoader
  }


  /**
   * Get an array of angular template view resources
   * @param request is the container's http request
   * @return an array of resources we want to output
   */
  protected Resource[] collectResources(ClassLoader cl) {

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl)

    Resource[] templates = resolver.getResources(getResourceMatchingPattern());

    return templates
  }

  protected Resource[] collectResources() {
    return collectResources(springScannerClassLoader)
  }
}
