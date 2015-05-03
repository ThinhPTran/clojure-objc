rm -Rf target/release
mkdir target/release
cp target/libclojure-objc.a target/release/
cp $J2OBJC_HOME/lib/libjre_emul.a target/release/
cp src/ffi/libffi.a target/release/
mkdir target/release/include
rsync -a $J2OBJC_HOME/include target/release
rsync -a target/include target/release
zip -r target/release.zip target/release/
