import SwiftUI

struct ApprovedHomeSunriseArtwork: View {
    var height: CGFloat = 100

    var body: some View {
        Image("approved-sunrise-reference")
            .resizable()
            .scaledToFill()
            .frame(maxWidth: .infinity)
            .frame(height: height)
            .clipped()
            .accessibilityHidden(true)
    }
}

struct SunriseArtwork: View {
    var height: CGFloat = 148
    var cornerRadius: CGFloat = 0
    var showsForest = false
    var showsLake = false
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
                        if points.count > 2 {
                            for index in 1..<(points.count - 1) {
                                let control = CGPoint(x: size.width * points[index].0, y: size.height * points[index].1)
                                let next = points[index + 1]
                                let midpoint = CGPoint(x: size.width * (points[index].0 + next.0) / 2,
                                                       y: size.height * (points[index].1 + next.1) / 2)
                                path.addQuadCurve(to: midpoint, control: control)
                            }
                        }
                        if let last = points.last {
                            path.addLine(to: CGPoint(x: size.width * last.0, y: size.height * last.1))
                        }
                        path.addLine(to: CGPoint(x: size.width, y: size.height))
                        path.addLine(to: CGPoint(x: 0, y: size.height))
                        path.closeSubpath()
                        context.fill(path, with: .color(color))
                    }

                    let clouds: [(CGFloat, CGFloat, CGFloat, Double)] = [(0.12, 0.31, 0.25, 0.20), (0.72, 0.27, 0.30, 0.16)]
                    for (x, y, width, opacity) in clouds {
                        let cloud = CGRect(x: size.width * x, y: size.height * y,
                                           width: size.width * width, height: size.height * 0.055)
                        context.fill(Path(ellipseIn: cloud), with: .color(Color(red: 1, green: 0.66, blue: 0.48).opacity(opacity)))
                    }
                    ridge([(0, 0.74), (0.13, 0.61), (0.24, 0.68), (0.40, 0.53), (0.53, 0.69),
                           (0.70, 0.57), (0.86, 0.69), (1, 0.56)], color: Color(red: 0.91, green: 0.65, blue: 0.57))
                    ridge([(0, 0.82), (0.17, 0.68), (0.32, 0.82), (0.48, 0.60), (0.66, 0.80),
                           (0.83, 0.65), (1, 0.80)], color: Color(red: 0.55, green: 0.62, blue: 0.68))

                    if showsLake {
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
                    }
                    if showsForest && showsLake {
                        let treeColor = Color(red: 0.12, green: 0.27, blue: 0.29)
                        let trees: [(CGFloat, CGFloat, CGFloat)] = [(0.02, 0.78, 0.18), (0.08, 0.80, 0.13), (0.14, 0.79, 0.10), (0.84, 0.80, 0.11), (0.91, 0.78, 0.16), (0.98, 0.79, 0.19)]
                        for (x, base, treeHeight) in trees {
                            let centerX = size.width * x
                            let baseY = size.height * base
                            let topY = baseY - size.height * treeHeight
                            var pine = Path()
                            pine.move(to: CGPoint(x: centerX, y: topY))
                            pine.addLine(to: CGPoint(x: centerX - size.width * treeHeight * 0.20, y: baseY - size.height * treeHeight * 0.25))
                            pine.addLine(to: CGPoint(x: centerX - size.width * treeHeight * 0.09, y: baseY - size.height * treeHeight * 0.25))
                            pine.addLine(to: CGPoint(x: centerX - size.width * treeHeight * 0.27, y: baseY - size.height * treeHeight * 0.03))
                            pine.addLine(to: CGPoint(x: centerX + size.width * treeHeight * 0.27, y: baseY - size.height * treeHeight * 0.03))
                            pine.addLine(to: CGPoint(x: centerX + size.width * treeHeight * 0.09, y: baseY - size.height * treeHeight * 0.25))
                            pine.addLine(to: CGPoint(x: centerX + size.width * treeHeight * 0.20, y: baseY - size.height * treeHeight * 0.25))
                            pine.closeSubpath()
                            context.fill(pine, with: .color(treeColor))
                        }
                    }
                    if showsLake { for index in 0..<5 {
                        let y = size.height * (0.86 + CGFloat(index) * 0.025)
                        var shimmer = Path()
                        shimmer.move(to: CGPoint(x: size.width * 0.43, y: y))
                        shimmer.addLine(to: CGPoint(x: size.width * 0.63, y: y))
                        context.stroke(shimmer, with: .color(Color.white.opacity(0.22 - Double(index) * 0.025)), lineWidth: 1)
                    } }
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
        }
        .frame(height: height)
        .accessibilityHidden(true)
    }
}
