(ns kosmos.orientdb.server.studio
  (:require [clojure.java.classpath :as classpath]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import  [java.nio.file FileSystems FileSystem Files CopyOption StandardCopyOption]))

(defn version [artifact]
  (let [pattern (re-pattern (str ".*/" artifact "-(.*?).jar"))
        [_ version] (->> (classpath/classpath-jarfiles)
                         (map (comp :name bean))
                         (keep (fn [jar] (re-matches pattern jar)))
                         first)]
    (or version "unknown")))

(defn not-dir? [dir]
  (or (not (.exists dir))
      (not (.isDirectory dir))))

(defn ensure-dir [dir]
  (when (not-dir? dir)
    (.mkdir dir)))

(defn download [source target]
  (try
    (log/infof "downloading link\n            from [%s]\n              to [%s]" source target)
    (with-open [is (.openStream (java.net.URL. source))
                os (io/output-stream target)]
      (io/copy is os))
    (catch Exception e
      (log/errorf "unable to download file from [%s] to [%s]" source target)
      (throw e)))
  (log/infof "file [%s] successfully downloaded" source))

(defn create-unique-tmp-dir!
  "Creates a unique temporary directory on the filesystem.
  Returns a java.io.File object pointing to the new directory.
  Raises an exception if the directory couldn't be created after 10000 tries."
  ([] (create-unique-tmp-dir! ""))
  ([name]
   (let [base-dir (System/getProperty "java.io.tmpdir")
         base-name (str (if-not (empty? name) name "dir") "-" (java.util.UUID/randomUUID))
         tmp-base (str base-dir java.io.File/separator base-name)
         max-attempts 10000]
     (loop [num-attempts 1]
       (if (= num-attempts max-attempts)
         (throw (Exception. (str "Failed to create temporary directory after " max-attempts " attempts.")))
         (let [tmp-dir-name (str tmp-base "-" num-attempts ".d")
               tmp-dir (io/as-file tmp-dir-name)]
           (if (.mkdir tmp-dir)
             tmp-dir
             (recur (inc num-attempts)))))))))

(defn mkpath [& paths]
  (str/join java.io.File/separator paths))

(defn fullpath [& paths]
  (str java.io.File/separator (apply mkpath paths)))

(defn delete-file [target]
  (let [target (io/as-file target)]
    (when (.isDirectory target)
      (doseq [f (.listFiles target)]
        (delete-file f)))
    (io/delete-file target)))

(defn- destination-path [sf df]
  (if (.isDirectory df)
    (.toPath (io/as-file (str df java.io.File/separator (.getName sf))))
    (.toPath df)))

(defn move [src dst]
  (let [sf (io/as-file src)
        df (io/as-file dst)]
    ;; This move method operates at the file system level and simply updates the
    ;; record with the new location of the file instead of copying all of the bits
    ;; from one location to another. For that reason, if an attempt is made to
    ;; move a file or directory across a disk partition,
    ;; java.nio.file.DirectoryNotEmptyException will be thrown
    ;; and this exception will in no way give you any information to help you
    ;; identify that this is the problem. To work around this problem, be sure
    ;; that the source and destination directories are on the same disc partition.
    (Files/move (.toPath sf) (destination-path sf df) (make-array CopyOption 0))))

(defn- unpack [entry stream dest-dir]
  (let [dest-file (io/as-file (str dest-dir java.io.File/separator (.getName entry)))]
    (when (not (.isDirectory entry))
      (.mkdirs (io/as-file (.getParent dest-file)))
      (io/copy stream dest-file))))

(defn expand [src to-dest-dir]
  (with-open [zis (java.util.zip.ZipInputStream. (io/input-stream (io/as-file src)))]
    (loop [entry (.getNextEntry zis)]
      (when entry
        (unpack entry zis to-dest-dir)
        (recur (.getNextEntry zis))))))


(def community-edition-deploy-link "https://search.maven.org/remotecontent?filepath=com/orientechnologies/orientdb-community/%1$s/orientdb-community-%1$s.zip")

(def studio-location "%1$s/orientdb-community-%2$s/plugins/orientdb-studio-%2$s.zip")

(defn download-community-edition-release [source target]
  (log/infof "downloading community release of orientdb [%s]" source)
  (download source target))

(defn unpack-community-edition-zip-file [source destination]
  (log/infof "unpacking [%s] to [%s]" source destination)
  (expand source destination))

(defn move-studio-file-into-place [source version target]
  (let [studio-file-location (format studio-location source version)]
    (log/infof "moving [%s] to [%s]" studio-file-location target)
    (move studio-file-location target)))

(defn ensure-studio-zip-file [orientdb-home]
  (log/info "initializing studio zip file")
  (let [plugins (mkpath orientdb-home "plugins")
        version (version "orientdb-server")
        parent (io/file plugins)
        target (io/file (format "%s/orientdb-studio-%s.zip" plugins version))]
    (ensure-dir parent)
    (if-not (.exists target)
      (let [_ (log/info "studio zip file not found ... downloading")
            tmp-dir (create-unique-tmp-dir!)
            _ (log/infof "tmp-dir = [%s]" tmp-dir)
            source (format community-edition-deploy-link version)
            download-file (format "%s/%s" tmp-dir (subs source (inc (.lastIndexOf source java.io.File/separator))))]
        (try
          (download-community-edition-release source download-file)
          (unpack-community-edition-zip-file download-file tmp-dir)
          (move-studio-file-into-place tmp-dir version target)
          (finally
            (delete-file tmp-dir))))
      (log/infof "found studio zip file ... continuing"))))
