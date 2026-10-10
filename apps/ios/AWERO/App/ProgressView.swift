import SwiftUI

struct AweroProgressView: View {
    let onSetAlarm: () -> Void
    let onOpenSettings: () -> Void

    private let ivory = AweroDesign.ivory
    private let navy = AweroDesign.navy
    private let coral = AweroDesign.coral

    var body: some View {
        NavigationStack {
            ZStack {
                ivory.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        HStack {
                            Text("AWERO")
                                .font(.system(size: 26, weight: .black))
                                .foregroundStyle(navy)
                            Spacer()
                            Button(action: onOpenSettings) {
                                Image(systemName: "gearshape")
                                    .font(.title3)
                                    .foregroundStyle(navy)
                                    .frame(minWidth: 44, minHeight: 44)
                            }
                            .accessibilityLabel(Text("settings.title"))
                        }
                        Text("progress.title")
                            .font(.system(size: 34, weight: .bold, design: .rounded))
                            .foregroundStyle(navy)
                            .accessibilityAddTraits(.isHeader)
                        VStack(alignment: .leading, spacing: 14) {
                            Image(systemName: "chart.bar.xaxis")
                                .font(.system(size: 38, weight: .semibold))
                                .foregroundStyle(coral)
                                .accessibilityHidden(true)
                            Text("progress.empty_title")
                                .font(.title2.bold())
                                .foregroundStyle(navy)
                                .multilineTextAlignment(.leading)
                            Text("progress.empty_body")
                                .font(.body)
                                .foregroundStyle(navy.opacity(0.68))
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(22)
                        .background(LinearGradient(colors: [.white, Color(red: 1, green: 0.95, blue: 0.88)], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .clipShape(RoundedRectangle(cornerRadius: 24))
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
                    .padding(.top, 2)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 12)
                    .padding(.bottom, 24)
                }
            }
            .toolbar(.hidden, for: .navigationBar)
        }
        .tint(coral)
    }
}
