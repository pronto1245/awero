import SwiftUI
import UIKit

/// Illustrated mountain-lake scenes used as AWERO page backgrounds. Home follows the time of
/// day; the wake screen always uses dawn. The night scene arrives with the dark theme.
enum AweroScene: String, CaseIterable {
    case dawn, day, sunset

    /// Dawn 05–11, day 11–18, sunset 18–05 (device local time).
    static func current(at date: Date = .now, calendar: Calendar = .current) -> AweroScene {
        switch calendar.component(.hour, from: date) {
        case 5..<11: return .dawn
        case 11..<18: return .day
        default: return .sunset
        }
    }

    /// Full-screen portrait artwork; its bottom fades into the ivory surface.
    var image: UIImage? { Self.images[self] ?? nil }

    // The scenes ship as loose JPEGs, which `Image(_ name:)` cannot resolve, so they are loaded by
    // path once and cached. A missing file falls back to the drawn sunrise.
    private static let images: [AweroScene: UIImage?] = Dictionary(
        uniqueKeysWithValues: allCases.map { scene in
            (scene, Bundle.main.path(forResource: "scene-\(scene.rawValue)-portrait", ofType: "jpg")
                .flatMap(UIImage.init(contentsOfFile:)))
        }
    )
}

/// Full-screen illustrated scene behind a page (Home, wake and mission screens). The artwork
/// fades into the ivory surface at the bottom, so it sits directly under the page content.
struct AweroSceneBackground: View {
    var scene: AweroScene = .dawn

    var body: some View {
        Group {
            if let image = scene.image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
                    .clipped()
            } else {
                VStack(spacing: 0) {
                    SunriseArtwork(height: 365, showsForest: true, showsLake: true)
                    AweroDesign.ivory
                }
            }
        }
        .accessibilityHidden(true)
    }
}
