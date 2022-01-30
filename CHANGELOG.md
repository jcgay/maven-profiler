# 3.2

- Restore sonarcloud analysis ([e75872c](http://github.com/jcgay/maven-profiler/commit/e75872cec8647bc1d6fdb20c6d8da8b23b70051))
- Remove deprecated code ([c096a4f](http://github.com/jcgay/maven-profiler/commit/c096a4febdcd8934a86163256fdb238e21da4e98))
- Set Locale when running test with Maven ([27c99b2](http://github.com/jcgay/maven-profiler/commit/27c99b20ed1ba7af8162fbba44609129026dec28))
- Bump Maven from 3.8.2 to 3.8.4 ([1e000e9](http://github.com/jcgay/maven-profiler/commit/1e000e978f2b5589ab43e2e05b185cf90a45915c))
- Add console reporter (writes to the System.out) ([d763fd7](http://github.com/jcgay/maven-profiler/commit/d763fd7e3e35f14b3ce680b21e38013cecb5865d)) by [@mgazanayi](https://github.com/mgazanayi)
- Hide user parameters in report ([9d80c3a](http://github.com/jcgay/maven-profiler/commit/9d80c3a0d2cd8e875b36a4232b9aebe93c97d0d8))

# 3.1.1
***

- Generate changelog with JReleaser ([99d3897](http://github.com/jcgay/maven-profiler/commit/99d3897c197e62c30dc862426653a2a59075e126))
- Report ITs in code coverage ([e17bf84](http://github.com/jcgay/maven-profiler/commit/e17bf84aaee99d2bf39a6c35d1e43e4470f4ccff))
- Activate profiling with mvnd ([61ff289](http://github.com/jcgay/maven-profiler/commit/61ff289f47d5228aa03e3b8266127cd80e0d659d))
- Build with GitHub actions ([944447c](http://github.com/jcgay/maven-profiler/commit/944447c3e9bd7ef5598d6e6b81f31845da870e4f))

# 3.0
***

- Add a profile name to produced reports ([af77d28](http://github.com/jcgay/maven-profiler/commit/af77d287f5681f888ca803e52edf907c6f069fc7)) by [@dmitry-timofeev](https://github.com/dmitry-timofeev)
- Migrate to Java 8 ([9dcb687](http://github.com/jcgay/maven-profiler/commit/9dcb68756e9801bf0dc21f88c8413fc775a9ad09))

# 2.6
***

- Customize reports directory ([06d6829](http://github.com/jcgay/maven-profiler/commit/06d6829082b19b4ef02344f168b2c06edafa75e9))

# 2.5
***

- Report global build time ([83fc997](http://github.com/jcgay/maven-profiler/commit/83fc9974f0dfb0d464ad6f662304d6959f72561e))
- Log when profiler is active and report directory ([5e21c3a](http://github.com/jcgay/maven-profiler/commit/5e21c3a529b928b4f98f0029fb65d8a12c5a45ad))
- Use event.getSession().getTopLevelProject() ([fbc8c7c](http://github.com/jcgay/maven-profiler/commit/fbc8c7ce18b108ecc81ce9b6395c0f0a2df85d84)) by [@jakub-bochenski](https://github.com/jakub-bochenski)
- profileFormat can now take multiple, comma-separated formats ([d2ccdb9](http://github.com/jcgay/maven-profiler/commit/d2ccdb9d7897b7735a0b57ed7a21ab54e4fc26cd)) by [@jakub-bochenski](https://github.com/jakub-bochenski)

# 2.4
***

- Can get times sorted by execution order ([03f28d7](http://github.com/jcgay/maven-profiler/commit/03f28d70195767f90495370d4dc94d20a141f782)) by [@higuaro](https://github.com/higuaro)

# 2.3
***

- Use milliseconds for JSON report ([99c46cd](http://github.com/jcgay/maven-profiler/commit/99c46cd5cde4324dc25714496b52e20a33f93116)) by [@BenjaminHerbert](https://github.com/BenjaminHerbert)

# 2.2
***

- Compatibility with new Maven [core extensions configuration mechanism](http://takari.io/2015/03/19/core-extensions.html) [view](http://github.com/jcgay/maven-profiler/commit/7cb7d431d39079b58d73731e30123ee25bf116c6)

# 2.1
***

- Minimize shaded JAR size [[view]](http://github.com/jcgay/maven-profiler/commit/70d5605c95beb7604491ef67c3e55dd5827e1388)
- Generate report in JSON [[view]](http://github.com/jcgay/maven-profiler/commit/f5b95067ee84af2b6934a76a98d30f0773d4cbf6) by [@jasongardnerlv](https://github.com/jasongardnerlv)
