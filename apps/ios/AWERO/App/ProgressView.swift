import SwiftUI

struct AweroProgressView: View {
    let onSetAlarm: () -> Void

    private let ivory = AweroDesign.ivory
    private let navy = AweroDesign.navy
    private let coral = AweroDesign.coral

    var body: some View {
        NavigationStack {
            ZStack {
                ivory.ignoresSafeArea()
                VStack(spacing: 22) {
                    Image(systemName: "chart.bar.xaxis")
                        .font(.system(size: 42, weight: .semibold))
                        .foregroundStyle(coral)
                        .accessibilityHidden(true)
                    Text("progress.empty_title")
                        .font(.title2.bold())
                        .foregroundStyle(navy)
                        .multilineTextAlignment(.center)
                    Text("progress.empty_body")
                        .font(.body)
                        .foregroundStyle(navy.opacity(0.68))
                        .multilineTextAlignment(.center)
                    Button(action: onSetAlarm) {
                        Text("progress.empty_action")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .foregroundStyle(AweroDesign.navy)
                            .background(coral)
                            .clipShape(RoundedRectangle(cornerRadius: 18))
                    }
                    .accessibilityIdentifier("progress.createAlarm")
                    .padding(.top, 8)
                }
                .padding(24)
                .frame(maxWidth: 420)
            }
            .navigationTitle("progress.title")
            .navigationBarTitleDisplayMode(.inline)
        }
        .tint(coral)
    }
}
