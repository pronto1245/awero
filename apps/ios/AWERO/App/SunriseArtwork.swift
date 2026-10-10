import SwiftUI

struct SunriseArtwork: View {
    var height: CGFloat = 148
    var cornerRadius: CGFloat = 0
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                LinearGradient(
                    colors: [
                        Color(red: 1.0, green: 0.71, blue: 0.55),
                        Color(red: 1.0, green: 0.84, blue: 0.62),
                        Color(red: 0.98, green: 0.89, blue: 0.75)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                Canvas { context, size in
                    let sunCenter = CGPoint(x: size.width * 0.53, y: size.height * 0.64)
                    let sunRect = CGRect(x: sunCenter.x - size.height * 0.30, y: sunCenter.y - size.height * 0.30,
                                         width: size.height * 0.60, height: size.height * 0.60)
                    context.fill(Path(ellipseIn: sunRect.insetBy(dx: -size.height * 0.10, dy: -size.height * 0.10)),
                                 with: .color(Color(red: 1, green: 0.79, blue: 0.43).opacity(0.18)))
                    context.fill(Path(ellipseIn: sunRect), with: .color(Color(red: 1, green: 0.76, blue: 0.34)))

                    func ridge(_ points: [(CGFloat, CGFloat)], color: Color) {
                        var path = Path()
                        guard let first = points.first else { return }
                        path.move(to: CGPoint(x: size.width * first.0, y: size.height * first.1))
                        for point in points.dropFirst() {
                            path.addLine(to: CGPoint(x: size.width * point.0, y: size.height * point.1))
                        }
                        path.addLine(to: CGPoint(x: size.width, y: size.height))
                        path.addLine(to: CGPoint(x: 0, y: size.height))
                        path.closeSubpath()
                        context.fill(path, with: .color(color))
                    }

                    ridge([(0, 0.74), (0.13, 0.56), (0.24, 0.67), (0.40, 0.47), (0.53, 0.68),
                           (0.70, 0.51), (0.86, 0.69), (1, 0.53)], color: Color(red: 0.91, green: 0.65, blue: 0.57))
                    ridge([(0, 0.82), (0.17, 0.63), (0.32, 0.82), (0.48, 0.55), (0.66, 0.79),
                           (0.83, 0.61), (1, 0.78)], color: Color(red: 0.55, green: 0.62, blue: 0.68))

                    var lake = Path()
                    lake.move(to: CGPoint(x: 0, y: size.height * 0.82))
                    lake.addCurve(to: CGPoint(x: size.width, y: size.height * 0.82),
                                  control1: CGPoint(x: size.width * 0.30, y: size.height * 0.75),
                                  control2: CGPoint(x: size.width * 0.67, y: size.height * 0.90))
                    lake.addLine(to: CGPoint(x: size.width, y: size.height))
                    lake.addLine(to: CGPoint(x: 0, y: size.height))
                    lake.closeSubpath()
                    context.fill(lake, with: .linearGradient(
                        Gradient(colors: [Color(red: 0.36, green: 0.51, blue: 0.50), Color(red: 0.60, green: 0.65, blue: 0.60)]),
                        startPoint: CGPoint(x: 0, y: size.height * 0.80),
                        endPoint: CGPoint(x: 0, y: size.height)
                    ))
                    for index in 0..<5 {
                        let y = size.height * (0.86 + CGFloat(index) * 0.025)
                        var shimmer = Path()
                        shimmer.move(to: CGPoint(x: size.width * 0.43, y: y))
                        shimmer.addLine(to: CGPoint(x: size.width * 0.63, y: y))
                        context.stroke(shimmer, with: .color(Color.white.opacity(0.22 - Double(index) * 0.025)), lineWidth: 1)
                    }
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
        }
        .frame(height: height)
        .accessibilityHidden(true)
    }
}
