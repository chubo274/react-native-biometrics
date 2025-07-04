require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "Biometrics"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => min_ios_version_supported }
  s.source       = { :git => "https://github.com/chubo274/react-native-biometrics.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm}"
  s.private_header_files = "ios/**/*.h"
  
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES'
  }

  s.frameworks = "LocalAuthentication"

  s.dependency "React-Core"

 install_modules_dependencies(s)
end
