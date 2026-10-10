import SwiftUI

struct AweroProgressView: View {
    let onSetAlarm: () -> Void
    let onOpenSettings: () -> Void

    private let ivory = AweroDesign.ivory
    private let navy = AweroDesign.navy
    private let coral = AweroDesign.coral
    @ScaledMetric(relativeTo: .largeTitle) private var titleSize: CGFloat = 34

    var body: some View {
        ZStack {
                ivory.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        HStack {
                            Text("AWERO")
                                .font(.system(size: 24, weight: .black))
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
                            .font(.system(size: titleSize, weight: .bold))
                            .foregroundStyle(navy)
                            .accessibilityAddTraits(.isHeader)
                        VStack(alignment: .leading, spacing: 14) {
                            Image(systemName: "leaf.fill")
                                .font(.system(size: 34, weight: .semibold))
                                .foregroundStyle(AweroDesign.sage)
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
                        .padding(20)
                        .background(Color(red: 0.93, green: 0.96, blue: 0.88))
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                    Button(action: onSetAlarm) {
                        Text("progress.empty_action")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .foregroundStyle(.white)
                            .background(coral)
                            .clipShape(RoundedRectangle(cornerRadius: AweroDesign.Corner.control))
                    }
                    .accessibilityIdentifier("progress.createAlarm")
                    .padding(.top, 2)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 12)
                    .padding(.bottom, 24)
                }
            }
        .tint(coral)
    }
}
