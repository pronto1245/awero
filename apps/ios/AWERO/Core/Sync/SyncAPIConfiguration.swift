import Foundation

struct SyncAPIConfiguration {
    private let baseComponents: URLComponents?

    var isEnabled: Bool { baseComponents != nil }

    init(value: String?) {
        guard let value = value?.trimmingCharacters(in: .whitespacesAndNewlines), !value.isEmpty,
              var components = URLComponents(string: value),
              components.scheme?.lowercased() == "https",
              let host = components.host, !host.isEmpty,
              components.user == nil, components.password == nil,
              components.query == nil, components.fragment == nil else {
            baseComponents = nil
            return
        }

        components.path = components.path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        baseComponents = components
    }

    init(bundle: Bundle = .main) {
        self.init(value: bundle.object(forInfoDictionaryKey: "AWERO_API_BASE_URL") as? String)
    }

    func endpoint(_ path: String) -> URL? {
        guard var components = baseComponents else { return nil }
        let relativePath = path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let segments = relativePath.split(separator: "/", omittingEmptySubsequences: false)
        guard !segments.isEmpty,
              segments.allSatisfy({ segment in
                  guard let decoded = String(segment).removingPercentEncoding else { return false }
                  return !decoded.isEmpty && decoded != "." && decoded != ".."
                      && !decoded.contains("?") && !decoded.contains("#")
                      && !decoded.contains("/") && !decoded.contains("\\")
              }) else { return nil }

        let prefix = components.path
        components.path = [prefix, relativePath].filter { !$0.isEmpty }.joined(separator: "/")
        return components.url
    }
}
