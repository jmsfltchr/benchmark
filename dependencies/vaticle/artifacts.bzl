#
# Copyright (C) 2021 Vaticle
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

load("@vaticle_dependencies//distribution/artifact:rules.bzl", "native_artifact_files")
load("@vaticle_dependencies//distribution:deployment.bzl", "deployment", "deployment_private")

def vaticle_typedb_artifacts():
    native_artifact_files(
        name = "vaticle_typedb_artifact",
        group_name = "vaticle_typedb",
        artifact_name = "typedb-server-{platform}-{version}.{ext}",
        tag_source = deployment["artifact.release"],
        commit_source = deployment["artifact.snapshot"],
        commit = "5706eaeab38a7580c9bc3baf7730456ee0ab262a",
    )

def vaticle_typedb_cluster_artifacts():
    native_artifact_files(
        name = "vaticle_typedb_cluster_artifact",
        group_name = "vaticle_typedb_cluster",
        artifact_name = "typedb-cluster-all-{platform}-{version}.{ext}",
        tag_source = deployment_private["artifact.release"],
        commit_source = deployment_private["artifact.snapshot"],
        commit = "566bba16b067e5ccfacd06dad3f05953c682f307",
    )
