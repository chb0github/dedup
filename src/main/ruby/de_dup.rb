require 'digest'

class DeDup

  @@hashes = { "md5" => Digest::MD5.new, "sha256" => Digest::SHA256.new }

  def initialize (roots =['.'], hashAlgo = 'MD5',fileTypes = ['.jpg','.gif'] )

    @hashAlgo =@@hashes[hashAlgo.downcase]
    @fileTypes = fileTypes
    @roots = roots.map { |r| r.tr('\\','/')}
  end

  def hash(filePath)
    @hashAlgo.reset
    @hashAlgo.file(filePath)
    @hashAlgo.hexdigest
  end

  def delete(file)
    File.delete(file)
    return true
  rescue e
    return false;
  end

  def go
    files = @roots.map { |d| get_files(d)}.flatten
    hashes = files.group_by {|f| hash(f)}
    hashes.values.map {|l| l[1,l.size]}.flatten.group_by{ |f| delete(f)}
  end

  def get_files(rootDir)
    base = rootDir + "/**/*"
    @fileTypes.map{|type|  base + type}.map {|p| Dir[p]}.flatten
  end

  app = DeDup::new(@roots = ["C:\\Users\\bongioc\\development\\nintex\\ncp-horizon\\samples\\pics"])
  puts app.go

end