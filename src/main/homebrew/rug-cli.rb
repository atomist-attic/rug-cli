class RugCli < Formula
  desc "Atomist rug command-line interface."
  homepage "https://github.com/atomisthq/rug-cli"
  url "https://#{ENV["ARTIFACTORY_USER"]}:#{ENV["ARTIFACTORY_TOKEN"]}@sforzando.artifactoryonline.com/sforzando/libs-release-local/com/atomist/rug-cli/${project.version}/rug-cli-${project.version}-bin.tar.gz"
  sha256 "${checksum}"
  head "https://github.com/atomisthq/rug-cli.git"

  bottle :unneeded

  depends_on :java => "1.8+"
  depends_on "maven"

  def install
    if build.head?
      system "mvn", "--settings", ".settings.xml", "-B", "-V", "package", "-DskipTests"
      libexec.install Dir["target/rug-cli-*-SNAPSHOT-bin/rug-cli-*-SNAPSHOT/*"]
    else
      libexec.install Dir["*"]
    end
    bin.install_symlink "#{libexec}/bin/rug"
    bash_completion.install "#{libexec}/etc/bash_completion.d/rug"
  end

  test do
    assert File.exist? "#{bin}/rug"
    if not build.head?
      assert_match version.to_s, shell_output("#{bin}/rug --version 2>&1", 0)
    end
  end
end
