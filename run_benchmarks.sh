TMPDIR="${PWD}/bundles"
if [ -d "$TMPDIR" ]; then
  # Take action if $DIR exists. #
  echo "removing ${TMPDIR} before running benchmarks"
  rm -rf ${TMPDIR}
fi
mkdir ${TMPDIR}

DIR="rpm-adapter"
if [ -d "$DIR" ]; then
  # Take action if $DIR exists. #
  echo "remove ${DIR} before running benchmarks"
  exit 0
fi
echo "Currently TMP: ${TMPDIR}"
cp /mnt/disk2/projects/rpmtests/bundle1000.tar.gz ${TMPDIR}
cp /mnt/disk2/projects/rpmtests/bundle100.tar.gz ${TMPDIR}

git clone https://github.com/artipie/rpm-adapter.git
cd rpm-adapter
echo ${PWD}
mvn package -Pbench
#wget https://artipie.s3.amazonaws.com/rpm-test/bundle100.tar.gz
#wget https://artipie.s3.amazonaws.com/rpm-test/bundle1000.tar.gz

mvn dependency:copy-dependencies
env BENCH_DIR=${TMPDIR} java -cp "target/benchmarks.jar:target/classes/*:target/dependency/*" org.openjdk.jmh.Main RpmBench
