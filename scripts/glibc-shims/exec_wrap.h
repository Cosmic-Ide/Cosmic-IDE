#ifndef COSMICIDE_EXEC_WRAP_H
#define COSMICIDE_EXEC_WRAP_H

/*
 * Internal environment keys shared by exec_wrap.c and libpath_redirect.c.
 *
 * EXEC_WRAP_EXECUTABLE_ENV contains the absolute executable path that should
 * be exposed as /proc/self/exe while a glibc program is launched explicitly
 * through the bundled dynamic loader.
 *
 * EXEC_WRAP_LOADER_ENV contains the absolute path of that loader. The path
 * redirect shim verifies the kernel-visible /proc/self/exe against this value
 * before applying the executable identity override, preventing stale inherited
 * variables from affecting processes that were not launched through the loader.
 */
#define EXEC_WRAP_EXECUTABLE_ENV "COSMIC_EXECUTABLE"
#define EXEC_WRAP_LOADER_ENV     "COSMIC_LOADER_EXECUTABLE"

#endif /* COSMICIDE_EXEC_WRAP_H */
